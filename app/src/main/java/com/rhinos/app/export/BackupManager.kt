package com.rhinos.app.export

import com.rhinos.app.data.model.DailyPnl
import com.rhinos.app.data.model.MarginType
import com.rhinos.app.data.model.Note
import com.rhinos.app.data.model.NoteCategory
import com.rhinos.app.data.model.Trade
import com.rhinos.app.data.model.TradeType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Serializable
data class TradeJson(
    val id: Long = 0,
    val stockName: String,
    val ticker: String,
    val type: String,
    val units: Int,
    val pricePerUnit: Double,
    val additionalCharges: Double,
    val dividend: Double,
    val tradeDate: String,
    val marginType: String,
    val expiryDate: String? = null,
    val notes: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
data class DailyPnlJson(
    val id: Long = 0,
    val date: String,
    val amount: Double,
    val notes: String = "",
    val createdAt: Long = 0
)

@Serializable
data class NoteJson(
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "GENERAL",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
data class BackupData(
    val app: String = "Rhinos",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val trades: List<TradeJson> = emptyList(),
    val dailyPnl: List<DailyPnlJson> = emptyList(),
    val notes: List<NoteJson> = emptyList()
)

/**
 * JSON export/import. The format is intentionally simple (see README) so users
 * can inspect their own data. Invalid rows are skipped rather than failing the
 * whole import.
 */
object BackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun export(
        trades: List<Trade>,
        daily: List<DailyPnl>,
        notes: List<Note>
    ): String = json.encodeToString(
        BackupData(
            trades = trades.map {
                TradeJson(
                    it.id, it.stockName, it.ticker, it.type.name, it.units, it.pricePerUnit,
                    it.additionalCharges, it.dividend, it.tradeDate.toString(), it.marginType.name,
                    it.expiryDate?.toString(), it.notes, it.createdAt, it.updatedAt
                )
            },
            dailyPnl = daily.map { DailyPnlJson(it.id, it.date.toString(), it.amount, it.notes, it.createdAt) },
            notes = notes.map { NoteJson(it.id, it.title, it.content, it.category.name, it.createdAt, it.updatedAt) }
        )
    )

    fun parse(text: String): BackupData = json.decodeFromString<BackupData>(text)

    /** Converts one exported trade into a domain object, or null if the row is invalid. */
    fun toTrade(row: TradeJson): Trade? {
        return try {
            val type = TradeType.valueOf(row.type.uppercase().trim())
            val margin = MarginType.valueOf(row.marginType.uppercase().trim())
            Trade(
                id = 0,
                stockName = row.stockName,
                ticker = row.ticker,
                type = type,
                units = row.units,
                pricePerUnit = row.pricePerUnit,
                additionalCharges = row.additionalCharges,
                dividend = row.dividend,
                tradeDate = LocalDate.parse(row.tradeDate),
                marginType = margin,
                expiryDate = row.expiryDate?.let { LocalDate.parse(it) },
                notes = row.notes,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt
            )
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException, is DateTimeParseException, is NullPointerException -> null
                else -> throw e
            }
        }
    }

    fun toDailyPnl(row: DailyPnlJson): DailyPnl? = try {
        DailyPnl(
            id = 0,
            date = LocalDate.parse(row.date),
            amount = row.amount,
            notes = row.notes,
            createdAt = row.createdAt
        )
    } catch (e: Exception) {
        if (e is IllegalArgumentException || e is DateTimeParseException || e is NullPointerException) null else throw e
    }

    fun toNote(row: NoteJson): Note? = try {
        Note(
            id = 0,
            title = row.title,
            content = row.content,
            category = NoteCategory.valueOf(row.category.uppercase().trim()),
            createdAt = row.createdAt,
            updatedAt = row.updatedAt
        )
    } catch (e: Exception) {
        if (e is IllegalArgumentException || e is DateTimeParseException || e is NullPointerException) null else throw e
    }
}
