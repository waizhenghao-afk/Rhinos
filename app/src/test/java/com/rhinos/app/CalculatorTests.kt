package com.rhinos.app

import com.rhinos.app.calculator.ExpiryCalculator
import com.rhinos.app.calculator.FifoEngine
import com.rhinos.app.calculator.NotEnoughUnitsException
import com.rhinos.app.calculator.StatsCalculator
import com.rhinos.app.calculator.TradeCalculator
import com.rhinos.app.data.model.MarginType
import com.rhinos.app.data.model.Trade
import com.rhinos.app.data.model.TradeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TradeCalculatorTest {

    @Test
    fun `buy cost adds charges`() {
        // 1000 x 10.20 + 8.50 = 10208.50
        assertEquals(10208.50, TradeCalculator.totalBuyCost(1000, 10.20, 8.50), 1e-9)
    }

    @Test
    fun `net sell value subtracts charges`() {
        // 1000 x 11.20 - 8.50 = 11191.50
        assertEquals(11191.50, TradeCalculator.netSellValue(1000, 11.20, 8.50), 1e-9)
    }

    @Test
    fun `gross value is units times price`() {
        assertEquals(10200.0, TradeCalculator.grossValue(1000, 10.20), 1e-9)
    }
}

class FifoEngineTest {

    private fun buy(
        id: Long, stock: String, units: Int, price: Double,
        charges: Double = 0.0, date: LocalDate = LocalDate.of(2026, 10, 1)
    ) = Trade(
        id = id, stockName = stock, ticker = stock, type = TradeType.BUY,
        units = units, pricePerUnit = price, additionalCharges = charges, dividend = 0.0,
        tradeDate = date, marginType = MarginType.PERMANENT, expiryDate = null
    )

    private fun sell(
        id: Long, stock: String, units: Int, price: Double,
        charges: Double = 0.0, date: LocalDate = LocalDate.of(2026, 10, 2)
    ) = Trade(
        id = id, stockName = stock, ticker = stock, type = TradeType.SELL,
        units = units, pricePerUnit = price, additionalCharges = charges, dividend = 0.0,
        tradeDate = date, marginType = MarginType.PERMANENT, expiryDate = null
    )

    @Test
    fun `one buy one sell`() {
        val trades = listOf(
            buy(1, "MAYBANK", 1000, 10.00),
            sell(2, "MAYBANK", 1000, 11.00)
        )
        val result = FifoEngine.analyze(trades)
        assertEquals(1, result.sells.size)
        assertEquals(1000.00, result.sells[0].realizedPnl, 1e-9)
    }

    @Test
    fun `partial sell keeps remaining position`() {
        val trades = listOf(
            buy(1, "MAYBANK", 1000, 10.00),
            sell(2, "MAYBANK", 400, 11.00)
        )
        val result = FifoEngine.analyze(trades)
        assertEquals(400.00, result.sells[0].realizedPnl, 1e-9)
        val position = result.positions["MAYBANK"]!!
        assertEquals(600, position.openUnits)
    }

    @Test
    fun `multiple buys one sell matches first lot then second`() {
        val trades = listOf(
            buy(1, "MAYBANK", 500, 10.00),
            buy(2, "MAYBANK", 500, 10.50),
            sell(3, "MAYBANK", 700, 12.00)
        )
        val result = FifoEngine.analyze(trades)
        // 500 x (12-10) + 200 x (12-10.5) = 1000 + 300 = 1300
        assertEquals(1300.00, result.sells[0].realizedPnl, 1e-9)
        assertEquals(300, result.positions["MAYBANK"]!!.openUnits)
    }

    @Test
    fun `one buy multiple sells`() {
        val trades = listOf(
            buy(1, "MAYBANK", 1000, 10.00),
            sell(2, "MAYBANK", 300, 11.00),
            sell(3, "MAYBANK", 700, 12.00)
        )
        val result = FifoEngine.analyze(trades)
        assertEquals(2, result.sells.size)
        assertEquals(300.00, result.sells[0].realizedPnl, 1e-9)
        assertEquals(1400.00, result.sells[1].realizedPnl, 1e-9)
        assertEquals(0, result.positions["MAYBANK"]!!.openUnits)
    }

    @Test
    fun `multiple buys multiple sells`() {
        val trades = listOf(
            buy(1, "MAYBANK", 500, 10.00),
            sell(2, "MAYBANK", 200, 11.00),
            buy(3, "MAYBANK", 500, 12.00),
            sell(4, "MAYBANK", 600, 13.00)
        )
        val result = FifoEngine.analyze(trades)
        // sell2: 200 x (11-10) = 200; sell4: 300 x (13-10) + 300 x (13-12) = 900+300 = 1200
        assertEquals(200.00, result.sells[0].realizedPnl, 1e-9)
        assertEquals(1200.00, result.sells[1].realizedPnl, 1e-9)
        assertEquals(200, result.positions["MAYBANK"]!!.openUnits)
    }

    @Test
    fun `charges are included in pnl`() {
        val trades = listOf(
            buy(1, "MAYBANK", 1000, 10.00, charges = 8.00),
            sell(2, "MAYBANK", 1000, 11.00, charges = 8.00)
        )
        val result = FifoEngine.analyze(trades)
        // (11000 - 8) - (10000 + 8) = 984
        assertEquals(984.00, result.sells[0].realizedPnl, 1e-9)
    }

    @Test
    fun `oversized sell throws`() {
        val trades = listOf(
            buy(1, "MAYBANK", 500, 10.00),
            sell(2, "MAYBANK", 600, 11.00)
        )
        try {
            FifoEngine.analyze(trades)
            throw AssertionError("expected NotEnoughUnitsException")
        } catch (e: NotEnoughUnitsException) {
            assertEquals(500, e.available)
            assertEquals(600, e.requested)
        }
    }

    @Test
    fun `breakeven trade`() {
        val trades = listOf(
            buy(1, "MAYBANK", 1000, 10.00),
            sell(2, "MAYBANK", 1000, 10.00)
        )
        val result = FifoEngine.analyze(trades)
        assertEquals(0.0, result.sells[0].realizedPnl, 1e-9)
        val stats = StatsCalculator.winStats(result.sells.map { it.realizedPnl })
        assertEquals(0, stats.winningTrades)
        assertEquals(0, stats.losingTrades)
        assertEquals(1, stats.breakevenTrades)
    }

    @Test
    fun `loss making trade`() {
        val trades = listOf(
            buy(1, "MAYBANK", 1000, 11.00),
            sell(2, "MAYBANK", 1000, 10.00)
        )
        val result = FifoEngine.analyze(trades)
        assertEquals(-1000.00, result.sells[0].realizedPnl, 1e-9)
    }

    @Test
    fun `tickers are independent`() {
        val trades = listOf(
            buy(1, "MAYBANK", 1000, 10.00),
            buy(2, "CIMB", 500, 6.00),
            sell(3, "MAYBANK", 200, 11.00)
        )
        val result = FifoEngine.analyze(trades)
        assertEquals(200.00, result.sells[0].realizedPnl, 1e-9)
        assertEquals(800, result.positions["MAYBANK"]!!.openUnits)
        assertEquals(500, result.positions["CIMB"]!!.openUnits)
    }

    @Test
    fun `older dated buys are consumed first regardless of list order`() {
        val trades = listOf(
            buy(2, "MAYBANK", 500, 10.50, date = LocalDate.of(2026, 10, 5)),
            buy(1, "MAYBANK", 500, 10.00, date = LocalDate.of(2026, 10, 1))
        )
        // sell is dated after BOTH buys so the full 1000 units are open to it
        val sale = sell(3, "MAYBANK", 700, 12.00, date = LocalDate.of(2026, 10, 6))
        val result = FifoEngine.analyze(trades + sale)
        // 500 x (12-10) + 200 x (12-10.5) = 1300 (oldest lot consumed first)
        assertEquals(1300.00, result.sells[0].realizedPnl, 1e-9)
    }

    @Test
    fun `matchSell reports per-lot detail`() {
        val buy1 = buy(1, "MAYBANK", 500, 10.00)
        val buy2 = buy(2, "MAYBANK", 500, 10.50)
        val sale = sell(3, "MAYBANK", 700, 12.00)
        val result = FifoEngine.matchSell(sale, listOf(buy1, buy2))
        assertEquals(700, result.matchedUnits)
        assertEquals(2, result.perLotPnl.size)
        assertEquals(1000.00, result.perLotPnl[0].pnl, 1e-9)
        assertEquals(300.00, result.perLotPnl[1].pnl, 1e-9)
    }
}

class ExpiryCalculatorTest {

    @Test
    fun `t plus 3 adds three calendar days`() {
        val tradeDate = LocalDate.of(2026, 10, 4)
        assertEquals(LocalDate.of(2026, 10, 7), ExpiryCalculator.expiryDate(tradeDate, MarginType.T_PLUS_3))
    }

    @Test
    fun `t plus 10 adds ten calendar days`() {
        val tradeDate = LocalDate.of(2026, 10, 4)
        assertEquals(LocalDate.of(2026, 10, 14), ExpiryCalculator.expiryDate(tradeDate, MarginType.T_PLUS_10))
    }

    @Test
    fun `permanent has no expiry`() {
        assertNull(ExpiryCalculator.expiryDate(LocalDate.of(2026, 10, 4), MarginType.PERMANENT))
    }

    @Test
    fun `status labels`() {
        val expiry = LocalDate.of(2026, 10, 7)
        assertTrue(ExpiryCalculator.status(expiry, LocalDate.of(2026, 10, 4)) is ExpiryCalculator.Status.Active)
        assertTrue(ExpiryCalculator.status(expiry, LocalDate.of(2026, 10, 6)) is ExpiryCalculator.Status.ExpiresTomorrow)
        assertTrue(ExpiryCalculator.status(expiry, LocalDate.of(2026, 10, 7)) is ExpiryCalculator.Status.ExpiresToday)
        assertTrue(ExpiryCalculator.status(expiry, LocalDate.of(2026, 10, 8)) is ExpiryCalculator.Status.Expired)
        assertTrue(ExpiryCalculator.status(null, LocalDate.of(2026, 10, 8)) is ExpiryCalculator.Status.NoExpiry)
    }

    @Test
    fun `days remaining`() {
        val expiry = LocalDate.of(2026, 10, 7)
        assertEquals(3, ExpiryCalculator.daysRemaining(expiry, LocalDate.of(2026, 10, 4)))
        assertEquals(-1, ExpiryCalculator.daysRemaining(expiry, LocalDate.of(2026, 10, 8)))
        assertNull(ExpiryCalculator.daysRemaining(null, LocalDate.of(2026, 10, 4)))
    }
}

class StatsCalculatorTest {

    @Test
    fun `win rate excludes breakeven from win and loss`() {
        val stats = StatsCalculator.winStats(listOf(100.0, -50.0, 200.0, 0.0))
        assertEquals(2, stats.winningTrades)
        assertEquals(1, stats.losingTrades)
        assertEquals(1, stats.breakevenTrades)
        assertEquals(4, stats.completedTrades)
        assertEquals(50.0, stats.winRatePercent!!, 1e-9)
    }

    @Test
    fun `win rate is null with no completed trades`() {
        val stats = StatsCalculator.winStats(emptyList())
        assertNull(stats.winRatePercent)
    }

    @Test
    fun `brief example 8 wins 4 losses`() {
        val pnls = List(8) { 10.0 } + List(4) { -5.0 }
        val stats = StatsCalculator.winStats(pnls)
        assertEquals(66.67, stats.winRatePercent!!, 0.01)
    }
}
