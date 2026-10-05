package com.rhinos.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhinos.app.AppContainer
import com.rhinos.app.export.BackupManager
import com.rhinos.app.notifications.ExpiryScheduler
import com.rhinos.app.settings.AppSettings
import com.rhinos.app.settings.ThemeMode
import com.rhinos.app.ui.components.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings: expiry tips, dark mode, currency, export/import (JSON), delete all
 * data. Everything is destructive-action-gated with confirmation dialogs.
 */
@Composable
fun SettingsScreen(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()

    var showDeleteAll by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf<android.net.Uri?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingImportText by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    var notifPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifPermissionGranted = granted
        if (!granted) {
            statusMessage = "Expiry tips are disabled because notification permission is off."
        }
    }

    // File pickers: SAF create-document / open-document, no storage permissions needed.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val trades = container.tradeRepository.getAllTrades()
                    val daily = container.dailyPnlRepository.getAllOnce()
                    val notes = container.noteRepository.getAllOnce()
                    val json = BackupManager.export(trades, daily, notes)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("Could not open file for writing.")
                    }
                    statusMessage = "Exported ${trades.size} trades, ${daily.size} daily records, ${notes.size} notes."
                } catch (e: Exception) {
                    statusMessage = "Export failed: ${e.message}"
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            input.readBytes().toString(Charsets.UTF_8)
                        } ?: error("Could not read the selected file.")
                    }
                    pendingImportText = text
                    pendingImportUri = uri
                    val parsed = BackupManager.parse(text)
                    statusMessage = "Ready to import: ${parsed.trades.size} trades, " +
                        "${parsed.dailyPnl.size} daily records, ${parsed.notes.size} notes."
                } catch (e: Exception) {
                    statusMessage = "Import failed: not a valid Rhinos backup file."
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Expiry tips
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Expiry Tips", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (notifPermissionGranted) "One gentle reminder the day before expiry"
                            else "Notification permission is off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.expiryTipsEnabled && notifPermissionGranted,
                        onCheckedChange = { enabled ->
                            if (enabled && !notifPermissionGranted) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                container.settingsRepository.setExpiryTipsEnabled(enabled)
                                scope.launch {
                                    val trades = container.tradeRepository.getAllTrades()
                                    ExpiryScheduler.rescheduleAll(context, trades, enabled)
                                }
                            }
                        }
                    )
                }
                if (!notifPermissionGranted) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Expiry tips are disabled because notification permission is off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Theme
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Dark Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { container.settingsRepository.setThemeMode(ThemeMode.SYSTEM) },
                        label = { Text("System") }
                    )
                    FilterChip(
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        onClick = { container.settingsRepository.setThemeMode(ThemeMode.LIGHT) },
                        label = { Text("Light") }
                    )
                    FilterChip(
                        selected = settings.themeMode == ThemeMode.DARK,
                        onClick = { container.settingsRepository.setThemeMode(ThemeMode.DARK) },
                        label = { Text("Dark") }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Currency
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Currency", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("RM" to "RM", "USD" to "$", "EUR" to "€", "SGD" to "S$").forEach { (code, symbol) ->
                        FilterChip(
                            selected = settings.currencyCode == code,
                            onClick = { container.settingsRepository.setCurrency(code, symbol) },
                            label = { Text(code) }
                        )
                    }
                }
                Text(
                    "Default is RM (Malaysian Ringgit).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Data
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "All data stays on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        exportLauncher.launch("rhinos-backup-${System.currentTimeMillis() / 1000}.json")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Export Data (JSON)") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import Data (JSON)") }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteAll = true },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete All Data") }
            }
        }

        statusMessage?.let {
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
        }

        // Hidden dev action: tap the version row 3 times to load sample data
        var devTapCount by remember { mutableStateOf(0) }
        Spacer(Modifier.height(16.dp))
        Text(
            "Rhinos v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .clickable {
                    devTapCount++
                    if (devTapCount >= 3) {
                        devTapCount = 0
                        scope.launch { loadSampleData(container) }
                        statusMessage = "Sample data loaded."
                    }
                }
        )
    }

    // Import confirmation: appends valid rows, skips invalid ones.
    if (pendingImportText != null && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportText = null; pendingImportUri = null },
            title = { Text("Import backup?") },
            text = {
                Text(
                    "Records from this file will be added to your data. " +
                        (statusMessage ?: "")
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val text = pendingImportText
                    pendingImportText = null
                    pendingImportUri = null
                    if (text == null) return@TextButton
                    scope.launch {
                        try {
                            val parsed = BackupManager.parse(text)
                            var tradesOk = 0
                            var dailyOk = 0
                            var notesOk = 0
                            parsed.trades.mapNotNull { BackupManager.toTrade(it) }
                                .also { tradesOk = it.size }
                                .let { container.tradeRepository.insertAllUnchecked(it) }
                            parsed.dailyPnl.mapNotNull { BackupManager.toDailyPnl(it) }
                                .also { dailyOk = it.size }
                                .let { container.dailyPnlRepository.insertAllUnchecked(it) }
                            parsed.notes.mapNotNull { BackupManager.toNote(it) }
                                .also { notesOk = it.size }
                                .let { container.noteRepository.insertAllUnchecked(it) }

                            val trades = container.tradeRepository.getAllTrades()
                            ExpiryScheduler.rescheduleAll(
                                context, trades,
                                container.settingsRepository.current().expiryTipsEnabled
                            )
                            statusMessage = "Imported $tradesOk trades, $dailyOk daily records, $notesOk notes."
                        } catch (e: Exception) {
                            statusMessage = "Import failed: ${e.message}"
                        }
                    }
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportText = null; pendingImportUri = null }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("Delete ALL data?") },
            text = {
                Text("Every trade, daily P&L record and note will be permanently removed. " +
                    "Consider exporting a backup first. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAll = false
                    scope.launch {
                        container.tradeRepository.deleteAllTrades()
                        container.dailyPnlRepository.deleteAll()
                        container.noteRepository.deleteAll()
                        val trades = container.tradeRepository.getAllTrades()
                        ExpiryScheduler.rescheduleAll(context, trades, false)
                        statusMessage = "All data deleted."
                    }
                }) { Text("Delete Everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAll = false }) { Text("Cancel") } }
        )
    }
}

/** Dev/test-only sample data. Accessed by tapping the version row 3 times. */
private suspend fun loadSampleData(container: AppContainer) {
    val maybankBuy = com.rhinos.app.data.model.Trade(
        stockName = "MAYBANK", ticker = "1155", type = com.rhinos.app.data.model.TradeType.BUY,
        units = 1000, pricePerUnit = 10.00, additionalCharges = 8.00, dividend = 0.0,
        tradeDate = java.time.LocalDate.now().minusDays(10),
        marginType = com.rhinos.app.data.model.MarginType.PERMANENT, expiryDate = null
    )
    val maybankSell = com.rhinos.app.data.model.Trade(
        stockName = "MAYBANK", ticker = "1155", type = com.rhinos.app.data.model.TradeType.SELL,
        units = 1000, pricePerUnit = 11.00, additionalCharges = 8.00, dividend = 0.0,
        tradeDate = java.time.LocalDate.now().minusDays(5),
        marginType = com.rhinos.app.data.model.MarginType.PERMANENT, expiryDate = null
    )
    val cimbBuy = com.rhinos.app.data.model.Trade(
        stockName = "CIMB", ticker = "1023", type = com.rhinos.app.data.model.TradeType.BUY,
        units = 500, pricePerUnit = 6.50, additionalCharges = 8.00, dividend = 0.0,
        tradeDate = java.time.LocalDate.now().minusDays(1),
        marginType = com.rhinos.app.data.model.MarginType.T_PLUS_10,
        expiryDate = java.time.LocalDate.now().plusDays(9)
    )
    try {
        container.tradeRepository.addTrade(maybankBuy)
        container.tradeRepository.addTrade(maybankSell)
        container.tradeRepository.addTrade(cimbBuy)
    } catch (e: Exception) {
        // sample data is best-effort
    }
}
