package com.rhinos.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class TradeType {
    BUY,
    SELL
}

enum class MarginType(val label: String) {
    T_PLUS_3("T+3"),
    T_PLUS_10("T+10"),
    PERMANENT("Permanent")
}

enum class NoteCategory(val label: String) {
    STRATEGY("Strategy"),
    TRADE_IDEA("Trade Idea"),
    LESSON("Lesson"),
    GENERAL("General")
}

@Entity(tableName = "trades")
data class Trade(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockName: String,
    val ticker: String,
    val type: TradeType,
    val units: Int,
    val pricePerUnit: Double,
    val additionalCharges: Double,
    val dividend: Double,
    val tradeDate: LocalDate,
    val marginType: MarginType,
    val expiryDate: LocalDate?,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_pnl")
data class DailyPnl(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val amount: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: NoteCategory = NoteCategory.GENERAL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
