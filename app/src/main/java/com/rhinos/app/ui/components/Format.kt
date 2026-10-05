package com.rhinos.app.ui.components

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * All number/date formatting funnels through here so currency stays consistent
 * app-wide and can be changed later without touching every screen.
 */
object Format {

    var currencySymbol: String = "RM"

    private val moneyFormatter: java.text.DecimalFormat = java.text.DecimalFormat("#,##0.00")

    /** "+RM 842.20" / "-RM 132.40" — sign always explicit for accessibility. */
    fun signedMoney(amount: Double): String {
        val sign = if (amount > 0.005) "+" else if (amount < -0.005) "-" else ""
        return "$sign${currencySymbol} ${moneyFormatter.format(kotlin.math.abs(amount))}"
    }

    fun money(amount: Double): String = "${currencySymbol} ${moneyFormatter.format(amount)}"

    fun plainMoney(amount: Double): String = moneyFormatter.format(amount)

    fun percent(value: Double?): String =
        if (value == null || value.isNaN() || value.isInfinite()) "—" else String.format(Locale.US, "%.1f%%", value)

    fun units(value: Int): String = String.format(Locale.US, "%,d", value)

    private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val shortFormat = DateTimeFormatter.ofPattern("d MMM yy", Locale.ENGLISH)

    fun date(date: LocalDate): String = date.format(dayFormat)

    fun shortDate(date: LocalDate): String = date.format(shortFormat)

    fun monthName(year: Int, month: Int): String =
        LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))

    fun yearLabel(year: Int): String = year.toString()

    /** Null-safe to avoid ever rendering "null"/"NaN" in the UI. */
    fun orDash(text: String?): String = text?.takeIf { it.isNotBlank() } ?: "—"
}
