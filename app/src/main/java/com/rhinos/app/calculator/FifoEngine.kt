package com.rhinos.app.calculator

import com.rhinos.app.data.model.Trade
import com.rhinos.app.data.model.TradeType
import java.time.LocalDate

/**
 * Result of FIFO-matching one SELL trade against the open BUY lots of a stock.
 *
 * @param matchedUnits total units matched against buy lots.
 * @param matchedBuyCost total cost of the matched buy units including their buy charges.
 * @param sellProceeds  net sale value: units x price minus the sell charges.
 * @param perLotPnl     realized P&L contribution per consumed buy lot.
 */
data class FifoResult(
    val matchedUnits: Int,
    val matchedBuyCost: Double,
    val sellProceeds: Double,
    val perLotPnl: List<LotPnl>
) {
    val realizedPnl: Double get() = sellProceeds - matchedBuyCost

    data class LotPnl(val buyTradeId: Long, val units: Int, val pnl: Double)
}

/** An open BUY lot with its per-unit cost including a share of the lot's charges. */
data class OpenLot(
    val tradeId: Long,
    val remainingUnits: Int,
    val pricePerUnit: Double,
    val chargesPerUnit: Double
) {
    val costPerUnit: Double get() = pricePerUnit + chargesPerUnit
}

class NotEnoughUnitsException(val available: Int, val requested: Int) :
    IllegalStateException("Not enough units available to sell. Available: $available, requested: $requested")

/**
 * Pure FIFO (First-In-First-Out) matching engine.
 *
 * Rules:
 *  - BUY trades open lots. Charges are spread over the lot's units so partial
 *    consumption matches proportionally.
 *  - SELL trades consume the oldest open lots first, oldest by trade date and,
 *    for equal dates, by insertion order.
 *  - Each ticker (or, if the ticker is blank, the stock name) is matched
 *    independently.
 *  - Selling more units than are open throws [NotEnoughUnitsException].
 */
object FifoEngine {

    data class Position(
        val ticker: String,
        val openUnits: Int,
        val openCost: Double,
        val realizedPnl: Double
    )

    fun keyOf(trade: Trade): String = trade.ticker.trim().ifBlank {
        trade.stockName.trim().uppercase()
    }

    /** All trades for one ticker, in the order they will be consumed. */
    fun processingOrder(trades: List<Trade>): List<Trade> =
        trades.sortedWith(
            compareBy({ it.tradeDate }, { it.createdAt }, { it.id })
        )

    fun availableUnits(buys: List<Trade>, sells: List<Trade>): Int {
        val buyUnits = buys.sumOf { it.units }
        val sellUnits = sells.sumOf { it.units }
        return buyUnits - sellUnits
    }

    /**
     * Matches one SELL against previously processed BUY trades of the same ticker.
     *
     * @param priorTrades every BUY/SELL of this ticker that chronologically precedes
     *        the sell being matched (the engine is not tolerant of trades inserted
     *        with historical dates behind an already-recorded sell; the repository
     *        recomputes downstream sells in that case).
     */
    fun matchSell(sell: Trade, priorTrades: List<Trade>): FifoResult {
        val ordered = processingOrder(priorTrades)
        val lots = ArrayDeque<OpenLot>()
        for (trade in ordered) {
            when (trade.type) {
                TradeType.BUY -> lots.addLast(
                    OpenLot(
                        tradeId = trade.id,
                        remainingUnits = trade.units,
                        pricePerUnit = trade.pricePerUnit,
                        chargesPerUnit = if (trade.units > 0) trade.additionalCharges / trade.units else 0.0
                    )
                )
                TradeType.SELL -> consume(lots, trade)
            }
        }
        return consumeAndReport(lots, sell)
    }

    /**
     * Runs the whole history and returns the open position plus the realized P&L
     * of every completed SELL trade. Used by the summary/reports.
     */
    fun analyze(trades: List<Trade>): FifoAnalysis {
        val ordered = processingOrder(trades)
        val lotsByTicker = mutableMapOf<String, ArrayDeque<OpenLot>>()
        val positions = mutableMapOf<String, Position>()
        val sellPnls = mutableListOf<AnalyzedSell>()

        for (trade in ordered) {
            val key = keyOf(trade)
            val lots = lotsByTicker.getOrPut(key) { ArrayDeque() }
            when (trade.type) {
                TradeType.BUY -> {
                    lots.addLast(
                        OpenLot(
                            tradeId = trade.id,
                            remainingUnits = trade.units,
                            pricePerUnit = trade.pricePerUnit,
                            chargesPerUnit = if (trade.units > 0) trade.additionalCharges / trade.units else 0.0
                        )
                    )
                    positions.merge(
                        key,
                        Position(key, trade.units, trade.units * trade.pricePerUnit + trade.additionalCharges, 0.0)
                    ) { old, add ->
                        Position(
                            old.ticker,
                            old.openUnits + add.openUnits,
                            old.openCost + add.openCost,
                            old.realizedPnl
                        )
                    }
                }
                TradeType.SELL -> {
                    val result = consumeAndReport(lots, trade)
                    sellPnls += AnalyzedSell(trade.id, key, result.realizedPnl, result.matchedUnits)
                    val current = positions.getOrPut(key) { Position(key, 0, 0.0, 0.0) }
                    positions[key] = current.copy(
                        openUnits = current.openUnits - result.matchedUnits,
                        openCost = current.openCost - result.matchedBuyCost,
                        realizedPnl = current.realizedPnl + result.realizedPnl
                    )
                }
            }
        }
        return FifoAnalysis(sellPnls, positions)
    }

    data class AnalyzedSell(
        val tradeId: Long,
        val ticker: String,
        val realizedPnl: Double,
        val units: Int
    )

    data class FifoAnalysis(
        val sells: List<AnalyzedSell>,
        val positions: Map<String, Position>
    )

    private fun consume(lots: ArrayDeque<OpenLot>, sell: Trade) {
        consumeAndReport(lots, sell)
    }

    private fun consumeAndReport(lots: ArrayDeque<OpenLot>, sell: Trade): FifoResult {
        var remaining = sell.units
        var matchedCost = 0.0
        val perLot = mutableListOf<FifoResult.LotPnl>()

        while (remaining > 0) {
            val lot = lots.firstOrNull()
            if (lot == null) {
                throw NotEnoughUnitsException(
                    available = sell.units - remaining,
                    requested = sell.units
                )
            }
            val take = minOf(remaining, lot.remainingUnits)
            val lotCost = take * lot.costPerUnit
            matchedCost += lotCost
            val lotPnl = take * sell.pricePerUnit - lotCost -
                (if (sell.units > 0) sell.additionalCharges * take / sell.units else 0.0)
            perLot += FifoResult.LotPnl(lot.tradeId, take, lotPnl)

            remaining -= take
            if (take == lot.remainingUnits) {
                lots.removeFirst()
            } else {
                lots[0] = lot.copy(remainingUnits = lot.remainingUnits - take)
            }
        }

        val proceeds = sell.units * sell.pricePerUnit - sell.additionalCharges
        return FifoResult(sell.units, matchedCost, proceeds, perLot)
    }
}
