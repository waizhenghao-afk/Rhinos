package com.rhinos.app.ui.trades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhinos.app.AppContainer
import com.rhinos.app.calculator.ExpiryCalculator
import com.rhinos.app.calculator.FifoEngine
import com.rhinos.app.calculator.NotEnoughUnitsException
import com.rhinos.app.calculator.TradeCalculator
import com.rhinos.app.data.model.MarginType
import com.rhinos.app.data.model.Trade
import com.rhinos.app.data.model.TradeType
import com.rhinos.app.notifications.ExpiryScheduler
import com.rhinos.app.ui.components.Format
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Record/Edit trade form. Shows the live buy-cost / net-sell calculation before
 * saving and blocks oversells with a friendly message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeForm(
    container: AppContainer,
    editing: Trade?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
    val trades by container.tradeRepository.observeTrades()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var stockName by remember { mutableStateOf(editing?.stockName ?: "") }
    var ticker by remember { mutableStateOf(editing?.ticker ?: "") }
    var type by remember { mutableStateOf(editing?.type ?: TradeType.BUY) }
    var unitsText by remember { mutableStateOf(editing?.units?.toString() ?: "") }
    var priceText by remember { mutableStateOf(editing?.pricePerUnit?.let { trimZero(it) } ?: "") }
    var chargesText by remember { mutableStateOf(editing?.additionalCharges?.let { trimZero(it) } ?: "") }
    var dividendText by remember { mutableStateOf(editing?.dividend?.let { trimZero(it) } ?: "") }
    var tradeDate by remember { mutableStateOf(editing?.tradeDate ?: LocalDate.now()) }
    var marginType by remember { mutableStateOf(editing?.marginType ?: MarginType.PERMANENT) }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val units = unitsText.toIntOrNull()
    val price = priceText.toDoubleOrNull()
    val charges = chargesText.toDoubleOrNull() ?: 0.0
    val dividend = dividendText.toDoubleOrNull() ?: 0.0

    val gross = if (units != null && price != null) TradeCalculator.grossValue(units, price) else null
    val netTotal = when {
        units == null || price == null -> null
        type == TradeType.BUY -> TradeCalculator.totalBuyCost(units, price, charges)
        else -> TradeCalculator.netSellValue(units, price, charges)
    }

    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            if (editing == null) "Record Trade" else "Edit Trade",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = stockName,
            onValueChange = { stockName = it },
            label = { Text("Stock / Investment Name") },
            isError = stockName.isBlank() && errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ticker,
            onValueChange = { ticker = it },
            label = { Text("Ticker Symbol") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == TradeType.BUY,
                onClick = { type = TradeType.BUY },
                label = { Text("BUY") }
            )
            FilterChip(
                selected = type == TradeType.SELL,
                onClick = { type = TradeType.SELL },
                label = { Text("SELL") }
            )
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = unitsText,
            onValueChange = { unitsText = it.filter { c -> c.isDigit() } },
            label = { Text("Number of Units") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                val avail = availableUnitsFor(trades, ticker.ifBlank { stockName }, editing?.id)
                if (type == TradeType.SELL) {
                    Text(if (avail != null) "Available: ${Format.units(avail)} units" else "Enter stock first")
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it.filterDecimal() },
            label = { Text("Price Per Unit") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = chargesText,
            onValueChange = { chargesText = it.filterDecimal() },
            label = { Text("Additional Charges") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = dividendText,
            onValueChange = { dividendText = it.filterDecimal() },
            label = { Text("Dividend") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Trade Date: ${Format.date(tradeDate)}")
        }
        Spacer(Modifier.height(12.dp))

        Text("Margin Period", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarginType.entries.forEach { margin ->
                FilterChip(
                    selected = marginType == margin,
                    onClick = { marginType = margin },
                    label = { Text(margin.label) }
                )
            }
        }
        val expiry = ExpiryCalculator.expiryDate(tradeDate, marginType)
        Text(
            if (expiry == null) "No expiry date" else "Expiry Date: ${Format.date(expiry)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // Live calculation preview
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    if (type == TradeType.BUY) "Buy Calculation" else "Sell Calculation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                CalcRow("Gross Value", gross)
                CalcRow("Charges", if (gross != null) charges else null)
                if (dividend > 0) CalcRow("Dividend", dividend)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (type == TradeType.BUY) "Total Cost" else "Net Sale",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    netTotal?.let { Format.money(it) } ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    errorMessage = null
                    val v = validate(stockName, units, price, charges, dividend)
                    if (v != null) {
                        errorMessage = v
                        return@Button
                    }
                    saving = true
                    val trade = Trade(
                        id = editing?.id ?: 0,
                        stockName = stockName.trim(),
                        ticker = ticker.trim(),
                        type = type,
                        units = units!!,
                        pricePerUnit = price!!,
                        additionalCharges = charges,
                        dividend = dividend,
                        tradeDate = tradeDate,
                        marginType = marginType,
                        expiryDate = expiry,
                        notes = notes.trim(),
                        createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    scope.launch {
                        try {
                            if (editing == null) container.tradeRepository.addTrade(trade)
                            else container.tradeRepository.updateTrade(trade)

                            // keep expiry alarms in sync with the database
                            val all = container.tradeRepository.getAllTrades()
                            ExpiryScheduler.rescheduleAll(
                                context,
                                all,
                                settings.expiryTipsEnabled
                            )
                            onSaved()
                            onDismiss()
                        } catch (e: NotEnoughUnitsException) {
                            errorMessage = "Not enough units available to sell."
                        } catch (e: IllegalArgumentException) {
                            errorMessage = e.message
                        } catch (e: Exception) {
                            errorMessage = "Could not save. Please check the values."
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.weight(1f)
            ) { Text(if (editing == null) "Save" else "Update") }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = tradeDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        tradeDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun CalcRow(label: String, value: Double?) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value?.let { Format.money(it) } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Units currently open for the given ticker, excluding the trade being edited. */
private fun availableUnitsFor(trades: List<Trade>, key: String, editingId: Long?): Int? {
    val clean = key.trim()
    if (clean.isBlank()) return null
    val matching = trades.filter { FifoEngine.keyOf(it) == clean.uppercase() && it.id != (editingId ?: -1L) }
    val buys = matching.filter { it.type == TradeType.BUY }.sumOf { it.units }
    val sells = matching.filter { it.type == TradeType.SELL }.sumOf { it.units }
    return buys - sells
}

internal fun String.filterDecimal(): String {
    val cleaned = filter { it.isDigit() || it == '.' }
    val firstDot = cleaned.indexOf('.')
    return if (firstDot == -1) cleaned
    else cleaned.take(firstDot + 1) + cleaned.substring(firstDot + 1).replace(".", "")
}

internal fun trimZero(d: Double): String =
    if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

private fun validate(
    stockName: String,
    units: Int?,
    price: Double?,
    charges: Double,
    dividend: Double
): String? = when {
    stockName.isBlank() -> "Stock name is required."
    units == null || units <= 0 -> "Units must be greater than 0."
    price == null || price < 0 -> "Price cannot be negative."
    charges < 0 -> "Charges cannot be negative."
    dividend < 0 -> "Dividend cannot be negative."
    else -> null
}
