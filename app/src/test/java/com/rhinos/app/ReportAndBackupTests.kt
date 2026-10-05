package com.rhinos.app

import com.rhinos.app.calculator.FifoEngine
import com.rhinos.app.calculator.ReportCalculator
import com.rhinos.app.calculator.StatsCalculator
import com.rhinos.app.data.model.DailyPnl
import com.rhinos.app.data.model.MarginType
import com.rhinos.app.data.model.Trade
import com.rhinos.app.data.model.TradeType
import com.rhinos.app.export.BackupManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportCalculatorTest {

    private fun trade(
        id: Long, stock: String, type: TradeType, units: Int, price: Double,
        date: LocalDate, dividend: Double = 0.0, charges: Double = 0.0
    ) = Trade(
        id = id, stockName = stock, ticker = stock, type = type, units = units,
        pricePerUnit = price, additionalCharges = charges, dividend = dividend,
        tradeDate = date, marginType = MarginType.PERMANENT, expiryDate = null
    )

    @Test
    fun `monthly report aggregates by sell date`() {
        val trades = listOf(
            trade(1, "MAYBANK", TradeType.BUY, 1000, 10.0, LocalDate.of(2026, 9, 1)),
            trade(2, "MAYBANK", TradeType.SELL, 1000, 11.0, LocalDate.of(2026, 10, 3)),
            trade(3, "CIMB", TradeType.BUY, 500, 5.0, LocalDate.of(2026, 10, 5), dividend = 120.0)
        )
        val daily = listOf(
            DailyPnl(date = LocalDate.of(2026, 10, 3), amount = 85.20),
            DailyPnl(date = LocalDate.of(2026, 10, 4), amount = -32.40),
            DailyPnl(date = LocalDate.of(2026, 9, 30), amount = 500.0)
        )
        val analysis = FifoEngine.analyze(trades)
        val sellPnl = analysis.sells.associate { it.tradeId to it.realizedPnl }
        val reports = ReportCalculator.buildReports(trades, daily, sellPnl)

        val october = reports.first { it.key.year == 2026 && it.key.month == 10 }
        assertEquals(1000.0, october.realizedPnl, 1e-9)
        assertEquals(85.20 - 32.40, october.dailyPnl, 1e-9)
        assertEquals(120.0, october.dividends, 1e-9)
        assertEquals(2, october.tradeCount)
        assertEquals(1, october.winningTrades)
        assertEquals(0, october.losingTrades)

        val september = reports.first { it.key.year == 2026 && it.key.month == 9 }
        assertEquals(0.0, september.realizedPnl, 1e-9)
        assertEquals(500.0, september.dailyPnl, 1e-9)
        assertEquals(1, september.tradeCount)
    }

    @Test
    fun `yearly report aggregates its months`() {
        val trades = listOf(
            trade(1, "MAYBANK", TradeType.BUY, 100, 10.0, LocalDate.of(2026, 1, 5)),
            trade(2, "MAYBANK", TradeType.SELL, 100, 12.0, LocalDate.of(2026, 2, 10)),
            trade(3, "CIMB", TradeType.BUY, 100, 10.0, LocalDate.of(2025, 1, 5)),
            trade(4, "CIMB", TradeType.SELL, 100, 9.0, LocalDate.of(2025, 2, 10))
        )
        val daily = listOf(
            DailyPnl(date = LocalDate.of(2026, 2, 1), amount = 100.0),
            DailyPnl(date = LocalDate.of(2025, 2, 1), amount = -50.0)
        )
        val analysis = FifoEngine.analyze(trades)
        val sellPnl = analysis.sells.associate { it.tradeId to it.realizedPnl }
        val reports = ReportCalculator.buildReports(trades, daily, sellPnl)

        val y2026 = reports.first { it.key.year == 2026 && it.key.month == null }
        assertEquals(200.0, y2026.realizedPnl, 1e-9)
        assertEquals(100.0, y2026.dailyPnl, 1e-9)
        assertEquals(1, y2026.winningTrades)

        val y2025 = reports.first { it.key.year == 2025 && it.key.month == null }
        assertEquals(-100.0, y2025.realizedPnl, 1e-9)
        assertEquals(-50.0, y2025.dailyPnl, 1e-9)
        assertEquals(1, y2025.losingTrades)
    }

    @Test
    fun `dividends use the record trade date`() {
        val trades = listOf(
            trade(1, "MAYBANK", TradeType.BUY, 100, 10.0, LocalDate.of(2026, 9, 20), dividend = 120.0)
        )
        val reports = ReportCalculator.buildReports(trades, emptyList(), emptyMap())
        val september = reports.first { it.key.month == 9 }
        assertEquals(120.0, september.dividends, 1e-9)
    }
}

class BackupManagerTest {

    @Test
    fun `export and re-import round trip`() {
        val trades = listOf(
            Trade(
                id = 1, stockName = "MAYBANK", ticker = "1155", type = TradeType.BUY,
                units = 1000, pricePerUnit = 10.0, additionalCharges = 8.0, dividend = 0.0,
                tradeDate = LocalDate.of(2026, 10, 4), marginType = MarginType.T_PLUS_3,
                expiryDate = LocalDate.of(2026, 10, 7), notes = "test note"
            )
        )
        val daily = listOf(
            DailyPnl(id = 1, date = LocalDate.of(2026, 10, 4), amount = 85.20, notes = "good day")
        )
        val notes = listOf(
            com.rhinos.app.data.model.Note(
                id = 1, title = "EMA strategy", content = "cross up",
                category = com.rhinos.app.data.model.NoteCategory.STRATEGY
            )
        )

        val json = BackupManager.export(trades, daily, notes)
        val parsed = BackupManager.parse(json)

        assertEquals(1, parsed.trades.size)
        assertEquals(1, parsed.dailyPnl.size)
        assertEquals(1, parsed.notes.size)

        val trade = BackupManager.toTrade(parsed.trades[0])!!
        assertEquals("MAYBANK", trade.stockName)
        assertEquals(LocalDate.of(2026, 10, 7), trade.expiryDate)
        assertEquals(1000, trade.units)

        val entry = BackupManager.toDailyPnl(parsed.dailyPnl[0])!!
        assertEquals(85.20, entry.amount, 1e-9)

        val note = BackupManager.toNote(parsed.notes[0])!!
        assertEquals("EMA strategy", note.title)
    }

    @Test
    fun `invalid rows return null instead of crashing`() {
        val badTrade = com.rhinos.app.export.TradeJson(
            stockName = "X", ticker = "X", type = "HOLD", units = 1,
            pricePerUnit = 1.0, additionalCharges = 0.0, dividend = 0.0,
            tradeDate = "2026-10-04", marginType = "T_PLUS_3"
        )
        assertNull(BackupManager.toTrade(badTrade))

        val badDate = com.rhinos.app.export.TradeJson(
            stockName = "X", ticker = "X", type = "BUY", units = 1,
            pricePerUnit = 1.0, additionalCharges = 0.0, dividend = 0.0,
            tradeDate = "not-a-date", marginType = "T_PLUS_3"
        )
        assertNull(BackupManager.toTrade(badDate))

        val badNote = com.rhinos.app.export.NoteJson(
            title = "t", content = "c", category = "NOT_A_CATEGORY"
        )
        assertNull(BackupManager.toNote(badNote))
    }

    @Test
    fun `parsing garbage text throws a catchable serialization exception`() {
        try {
            BackupManager.parse("this is not json")
            throw AssertionError("expected exception")
        } catch (e: Exception) {
            // expected: SerializationException / IllegalArgumentException family
            assertNotNull(e)
        }
    }
}
