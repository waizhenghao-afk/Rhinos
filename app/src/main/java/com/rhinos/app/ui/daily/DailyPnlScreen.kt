package com.rhinos.app.ui.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhinos.app.AppContainer
import com.rhinos.app.data.model.DailyPnl
import com.rhinos.app.ui.components.EmptyState
import com.rhinos.app.ui.components.Format
import com.rhinos.app.ui.components.PnlText
import com.rhinos.app.ui.theme.pnlColor
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Manual daily portfolio performance journal. Kept strictly separate from
 * realized trading P&L and dividends.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPnlScreen(container: AppContainer) {
    val entries by container.dailyPnlRepository.observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
    Format.currencySymbol = settings.currencySymbol

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DailyPnl?>(null) }
    var deleting by remember { mutableStateOf<DailyPnl?>(null) }

    val today = remember { LocalDate.now() }
    val todayTotal = remember(entries, today) {
        entries.filter { it.date == today }.sumOf { it.amount }
    }
    val weekStart = remember(today) { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val weekTotal = remember(entries, weekStart, today) {
        entries.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(today) }.sumOf { it.amount }
    }
    val monthTotal = remember(entries, today) {
        entries.filter { it.date.year == today.year && it.date.monthValue == today.monthValue }.sumOf { it.amount }
    }
    val allTotal = remember(entries) { entries.sumOf { it.amount } }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Daily P&L", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Manual journal of daily portfolio performance",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        Button(onClick = { editing = null; showForm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Record Daily P&L")
        }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.rhinos.app.ui.components.SummaryCard(
                label = "Today",
                value = Format.signedMoney(todayTotal),
                modifier = Modifier.weight(1f),
                valueColor = pnlColor(todayTotal >= 0)
            )
            com.rhinos.app.ui.components.SummaryCard(
                label = "This Week",
                value = Format.signedMoney(weekTotal),
                modifier = Modifier.weight(1f),
                valueColor = pnlColor(weekTotal >= 0)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.rhinos.app.ui.components.SummaryCard(
                label = "This Month",
                value = Format.signedMoney(monthTotal),
                modifier = Modifier.weight(1f),
                valueColor = pnlColor(monthTotal >= 0)
            )
            com.rhinos.app.ui.components.SummaryCard(
                label = "Total",
                value = Format.signedMoney(allTotal),
                modifier = Modifier.weight(1f),
                valueColor = pnlColor(allTotal >= 0)
            )
        }

        Spacer(Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                EmptyState(
                    title = "No daily P&L recorded",
                    message = "Record each day's profit or loss to build a performance journal."
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { it.id }) { entry ->
                    DailyCard(
                        entry = entry,
                        onEdit = { editing = entry; showForm = true },
                        onDelete = { deleting = entry }
                    )
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }

    if (showForm) {
        ModalBottomSheet(onDismissRequest = { showForm = false }) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                DailyPnlForm(container, editing = editing, onDismiss = { showForm = false })
            }
        }
    }

    deleting?.let { entry ->
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete entry?") },
            text = { Text("Delete the P&L for ${Format.date(entry.date)}?") },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = entry
                    deleting = null
                    scope.launch { container.dailyPnlRepository.delete(toDelete) }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DailyCard(entry: DailyPnl, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(Format.date(entry.date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (entry.notes.isNotBlank()) {
                    Text(
                        entry.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            PnlText(amount = entry.amount, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit entry") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete entry") }
        }
    }
}

/** Add/edit form for one daily P&L record. Accepts signed amounts like -32.40. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPnlForm(
    container: AppContainer,
    editing: DailyPnl?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var date by remember { mutableStateOf(editing?.date ?: LocalDate.now()) }
    var amountText by remember { mutableStateOf(editing?.amount?.let { trimZero(it) } ?: "") }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(
            if (editing == null) "Record Daily P&L" else "Edit Daily P&L",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Date: ${Format.date(date)}")
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { input ->
                amountText = input.filter { it.isDigit() || it == '.' || it == '-' }.let { cleaned ->
                    // keep only a leading minus and single dot
                    var seenDot = false
                    val out = StringBuilder()
                    cleaned.forEachIndexed { i, c ->
                        when {
                            c == '-' && i == 0 -> out.append(c)
                            c == '.' && !seenDot -> { seenDot = true; out.append(c) }
                            c.isDigit() -> out.append(c)
                        }
                    }
                    out.toString()
                }
            },
            label = { Text("Profit / Loss amount (+ or -)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    errorMessage = null
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount.isNaN() || amount.isInfinite()) {
                        errorMessage = "Enter a valid amount, e.g. 85.20 or -32.40"
                        return@Button
                    }
                    val entry = DailyPnl(
                        id = editing?.id ?: 0,
                        date = date,
                        amount = amount,
                        notes = notes.trim(),
                        createdAt = editing?.createdAt ?: System.currentTimeMillis()
                    )
                    scope.launch {
                        try {
                            if (editing == null) container.dailyPnlRepository.add(entry)
                            else container.dailyPnlRepository.update(entry)
                            onDismiss()
                        } catch (e: IllegalArgumentException) {
                            errorMessage = e.message
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (editing == null) "Save" else "Update") }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}

private fun trimZero(d: Double): String =
    if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
