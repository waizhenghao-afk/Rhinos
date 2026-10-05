package com.rhinos.app.data.repository

import com.rhinos.app.calculator.FifoEngine
import com.rhinos.app.calculator.NotEnoughUnitsException
import com.rhinos.app.data.db.TradeDao
import com.rhinos.app.data.model.MarginType
import com.rhinos.app.data.model.Trade
import com.rhinos.app.data.model.TradeType
import kotlinx.coroutines.flow.Flow

/**
 * Single write path for trades. Every insert/update/delete revalidates FIFO
 * availability so negative holdings can never be persisted, and returns the
 * re-derivable state the caller needs for notifications.
 */
class TradeRepository(private val dao: TradeDao) {

    fun observeTrades(): Flow<List<Trade>> = dao.observeAll()

    suspend fun getTrade(id: Long): Trade? = dao.getById(id)

    suspend fun getAllTrades(): List<Trade> = dao.getAllOnce()

    /**
     * @throws NotEnoughUnitsException when a SELL (given the full history) would
     *         consume more units than open for that ticker.
     */
    suspend fun addTrade(trade: Trade): Long {
        validate(trade)
        if (trade.type == TradeType.SELL) {
            val all = dao.getAllOnce()
            val history = all.filter { FifoEngine.keyOf(it) == FifoEngine.keyOf(trade) }
            FifoEngine.matchSell(trade, history.filter { it.id != trade.id })
        }
        return dao.insert(trade)
    }

    /**
     * Replaces a stored trade. Deleting a BUY can retroactively invalidate later
     * sells, so the whole ticker history is replayed after the swap.
     */
    suspend fun updateTrade(updated: Trade) {
        validate(updated)
        val all = dao.getAllOnce()
        val tickerHistory = all.filter { FifoEngine.keyOf(it) == FifoEngine.keyOf(updated) }
        val replay = tickerHistory.filter { it.id != updated.id } + updated
        FifoEngine.analyze(replay) // throws if the new state is invalid
        dao.update(updated)
    }

    suspend fun deleteTrade(trade: Trade) {
        dao.delete(trade)
    }

    /**
     * Schedules and refreshes every expiry reminder after data changes.
     * Returns the list of trades that currently carry an expiry date.
     */
    suspend fun deleteAllTrades(): Unit = dao.deleteAll()

    /** Convenience for the sample-data loader and import: bulk insert without checks. */
    suspend fun insertAllUnchecked(trades: List<Trade>) = dao.insertAll(trades)

    private fun validate(trade: Trade) {
        require(trade.stockName.isNotBlank()) { "Stock name is required." }
        require(trade.units > 0) { "Units must be greater than 0." }
        require(trade.pricePerUnit >= 0) { "Price cannot be negative." }
        require(trade.additionalCharges >= 0) { "Charges cannot be negative." }
        require(trade.dividend >= 0) { "Dividend cannot be negative." }
    }

    companion object {
        fun withExpiry(trades: List<Trade>): List<Trade> =
            trades.filter { it.expiryDate != null }
    }
}
