package com.rhinos.app.calculator

import com.rhinos.app.data.model.DailyPnl
import com.rhinos.app.data.model.MarginType
import com.rhinos.app.data.model.Trade
import com.rhinos.app.data.model.TradeType
import java.time.LocalDate

/** Buy/sell arithmetic for a single transaction. */
object TradeCalculator {

    /** Gross value = units x price, before charges. */
    fun grossValue(units: Int, pricePerUnit: Double): Double = units * pricePerUnit

    /** Total buy cost = gross + charges. */
    fun totalBuyCost(units: Int, pricePerUnit: Double, charges: Double): Double =
        grossValue(units, pricePerUnit) + charges

    /** Net sell value = gross - charges. */
    fun netSellValue(units: Int, pricePerUnit: Double, charges: Double): Double =
        grossValue(units, pricePerUnit) - charges
}

/** Expiry date and status labels for the margin feature. */
object ExpiryCalculator {

    /** Calendar-day arithmetic, deliberately NOT exchange settlement rules. */
    fun expiryDate(tradeDate: LocalDate, marginType: MarginType): LocalDate? = when (marginType) {
        MarginType.T_PLUS_3 -> tradeDate.plusDays(3)
        MarginType.T_PLUS_10 -> tradeDate.plusDays(10)
        MarginType.PERMANENT -> null
    }

    sealed interface Status {
        data object Active : Status               // more than 1 day remaining
        data class ExpiresInDays(val days: Int) : Status
        data object ExpiresTomorrow : Status
        data object ExpiresToday : Status
        data object Expired : Status
        data object NoExpiry : Status
    }

    fun status(expiryDate: LocalDate?, today: LocalDate): Status {
        if (expiryDate == null) return Status.NoExpiry
        return when {
            today.isBefore(expiryDate.minusDays(1)) -> Status.Active
            today == expiryDate.minusDays(1) -> Status.ExpiresTomorrow
            today == expiryDate -> Status.ExpiresToday
            else -> Status.Expired
        }
    }

    fun daysRemaining(expiryDate: LocalDate?, today: LocalDate): Int? {
        if (expiryDate == null) return null
        return java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate).toInt()
    }
}

/** Win/loss statistics over completed (SELL) trades. */
object StatsCalculator {

    data class WinStats(
        val completedTrades: Int,
        val winningTrades: Int,
        val losingTrades: Int,
        val breakevenTrades: Int,
        val winRatePercent: Double?
    ) {
        companion object {
            val EMPTY = WinStats(0, 0, 0, 0, null)
        }
    }

    private const val EPSILON = 0.005 // half a cent: only meaningful sub-cent rounding counts as breakeven

    /**
     * Win rate = winning / completed x 100. Breakeven trades are counted separately
     * and excluded from both winning and losing. Open BUY positions never count.
     */
    fun winStats(sellPnls: List<Double>): WinStats {
        var winning = 0
        var losing = 0
        var breakeven = 0
        for (pnl in sellPnls) {
            when {
                pnl > EPSILON -> winning++
                pnl < -EPSILON -> losing++
                else -> breakeven++
            }
        }
        val completed = winning + losing + breakeven
        val rate = if (completed == 0) null else winning.toDouble() / completed * 100.0
        return WinStats(completed, winning, losing, breakeven, rate)
    }
}

/** Aggregates trades/daily records by month and year for the Reports page. */
object ReportCalculator {

    private const val EPSILON = 0.005 // same threshold as StatsCalculator

    data class PeriodKey(val year: Int, val month: Int?)

    data class PeriodReport(
        val key: PeriodKey,
        val realizedPnl: Double,
        val dailyPnl: Double,
        val dividends: Double,
        val tradeCount: Int,          // all BUY+SELL trades dated in period
        val winningTrades: Int,
        val losingTrades: Int,
        val breakevenTrades: Int,
        val completedTrades: Int,
        val winRatePercent: Double?
    ) {
        val isYear: Boolean get() = key.month == null
    }

    /**
     * Realized P&L and win/loss counts are attributed to the SELL trade's date.
     * Dividends use the trade date of the record carrying them. Daily P&L uses
     * its own record date. Records from different years never mix.
     */
    fun buildReports(
        trades: List<Trade>,
        daily: List<DailyPnl>,
        sellPnlByTradeId: Map<Long, Double>
    ): List<PeriodReport> {
        val byPeriod = mutableMapOf<PeriodKey, MutableReport>()

        for (trade in trades) {
            val monthKey = PeriodKey(trade.tradeDate.year, trade.tradeDate.monthValue)
            val yearKey = PeriodKey(trade.tradeDate.year, null)

            if (trade.type == TradeType.SELL) {
                val pnl = sellPnlByTradeId[trade.id] ?: 0.0
                val month = byPeriod.getOrPut(monthKey) { MutableReport(monthKey) }
                month.realizedPnl += pnl
                month.completedTrades++
                when {
                    pnl > EPSILON -> month.winningTrades++
                    pnl < -EPSILON -> month.losingTrades++
                    else -> month.breakevenTrades++
                }
                val year = byPeriod.getOrPut(yearKey) { MutableReport(yearKey) }
                year.realizedPnl += pnl
                year.completedTrades++
                when {
                    pnl > EPSILON -> year.winningTrades++
                    pnl < -EPSILON -> year.losingTrades++
                    else -> year.breakevenTrades++
                }
            } else {
                byPeriod.getOrPut(monthKey) { MutableReport(monthKey) }
                byPeriod.getOrPut(yearKey) { MutableReport(yearKey) }
            }

            byPeriod.getValue(monthKey).tradeCount++
            byPeriod.getValue(yearKey).tradeCount++
            if (trade.dividend > 0.0) {
                byPeriod.getValue(monthKey).dividends += trade.dividend
                byPeriod.getValue(yearKey).dividends += trade.dividend
            }
        }

        for (entry in daily) {
            val monthKey = PeriodKey(entry.date.year, entry.date.monthValue)
            val yearKey = PeriodKey(entry.date.year, null)
            byPeriod.getOrPut(monthKey) { MutableReport(monthKey) }.dailyPnl += entry.amount
            byPeriod.getOrPut(yearKey) { MutableReport(yearKey) }.dailyPnl += entry.amount
        }

        return byPeriod.values
            .map { m ->
                PeriodReport(
                    key = m.key,
                    realizedPnl = round2(m.realizedPnl),
                    dailyPnl = round2(m.dailyPnl),
                    dividends = round2(m.dividends),
                    tradeCount = m.tradeCount,
                    winningTrades = m.winningTrades,
                    losingTrades = m.losingTrades,
                    breakevenTrades = m.breakevenTrades,
                    completedTrades = m.completedTrades,
                    winRatePercent = if (m.completedTrades == 0) null
                    else m.winningTrades.toDouble() / m.completedTrades * 100.0
                )
            }
            .sortedWith(compareByDescending<PeriodReport> { it.key.year }.thenByDescending { it.key.month ?: 12 })
    }

    private class MutableReport(val key: PeriodKey) {
        var realizedPnl = 0.0
        var dailyPnl = 0.0
        var dividends = 0.0
        var tradeCount = 0
        var winningTrades = 0
        var losingTrades = 0
        var breakevenTrades = 0
        var completedTrades = 0
    }

    private fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0
}
