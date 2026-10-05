package com.rhinos.app.ui.trades

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhinos.app.AppContainer
import com.rhinos.app.calculator.FifoEngine
import com.rhinos.app.data.model.Trade
import com.rhinos.app.data.model.TradeType
import com.rhinos.app.notifications.ExpiryScheduler
import com.rhinos.app.ui.components.DirectionIcon
import com.rhinos.app.ui.components.EmptyState
import com.rhinos.app.ui.components.Format
import com.rhinos.app.ui.components.PnlText
import kotlinx.coroutines.launch

/**
 * All recorded trades with filters (stock, buy/sell, month, year) and
 * edit/delete with confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradesScreen(container: AppContainer) {
    val context = LocalContext.current
    val trades by container.tradeRepository.observeTrades()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
    Format.currencySymbol = settings.currencySymbol

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Trade?>(null) }
    var deleting by remember { mutableStateOf<Trade?>(null) }

    var filterStock by remember { mutableStateOf<String?>(null) }
    var filterType by remember { mutableStateOf<TradeType?>(null) }
    var filterMonth by remember { mutableStateOf<Int?>(null) }
    var filterYear by remember { mutableStateOf<Int?>(null) }

    val scope = rememberCoroutineScope()

    val analysis = remember(trades) { FifoEngine.analyze(trades) }
    val pnlByTrade = remember(analysis) {
        analysis.sells.associate { it.tradeId to it.realizedPnl }
    }

    val stocks = remember(trades) { trades.map { it.stockName }.distinct().sorted() }
    val years = remember(trades) { trades.map { it.tradeDate.year }.distinct().sortedDescending() }

    val filtered = remember(trades, filterStock, filterType, filterMonth, filterYear) {
        trades.filter { t ->
            (filterStock == null || t.stockName == filterStock) &&
                (filterType == null || t.type == filterType) &&
                (filterMonth == null || t.tradeDate.monthValue == filterMonth) &&
                (filterYear == null || t.tradeDate.year == filterYear)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Trades", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(12.dp))

        Button(onClick = { editing = null; showForm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Record Trade")
        }

        Spacer(Modifier.height(12.dp))

        if (stocks.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterStock == null,
                    onClick = { filterStock = null },
                    label = { Text("All Stocks") }
                )
                stocks.forEach { s ->
                    FilterChip(
                        selected = filterStock == s,
                        onClick = { filterStock = s },
                        label = { Text(s) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        if (years.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterYear == null,
                    onClick = { filterYear = null },
                    label = { Text("All Years") }
                )
                years.forEach { y ->
                    FilterChip(
                        selected = filterYear == y,
                        onClick = { filterYear = y },
                        label = { Text(y.toString()) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = filterType == TradeType.BUY,
                onClick = {
                    filterType = if (filterType == TradeType.BUY) null else TradeType.BUY
                },
                label = { Text("BUY") }
            )
            FilterChip(
                selected = filterType == TradeType.SELL,
                onClick = {
                    filterType = if (filterType == TradeType.SELL) null else TradeType.SELL
                },
                label = { Text("SELL") }
            )
            FilterChip(
                selected = filterMonth != null,
                onClick = {
                    filterMonth = if (filterMonth != null) null else java.time.LocalDate.now().monthValue
                },
                label = { Text("This Month") }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (trades.isEmpty()) {
            EmptyState(
                title = "No trades yet",
                message = "Record your first investment to start tracking your performance."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { trade ->
                    TradeCard(
                        trade = trade,
                        pnl = pnlByTrade[trade.id],
                        onEdit = { editing = trade; showForm = true },
                        onDelete = { deleting = trade }
                    )
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }

    if (showForm) {
        ModalBottomSheet(onDismissRequest = { showForm = false }) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                TradeForm(
                    container = container,
                    editing = editing,
                    onDismiss = { showForm = false }
                )
            }
        }
    }

    deleting?.let { trade ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete trade?") },
            text = { Text("Delete the ${trade.type.name} of ${trade.stockName}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = trade
                    deleting = null
                    scope.launch {
                        container.tradeRepository.deleteTrade(toDelete)
                        val all = container.tradeRepository.getAllTrades()
                        ExpiryScheduler.rescheduleAll(
                            context,
                            all,
                            container.settingsRepository.current().expiryTipsEnabled
                        )
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TradeCard(
    trade: Trade,
    pnl: Double?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DirectionIcon(trade.type)
                    Spacer(Modifier.padding(start = 6.dp))
                    Text(
                        trade.stockName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (trade.ticker.isNotBlank()) {
                        Spacer(Modifier.padding(start = 6.dp))
                        Text(
                            trade.ticker,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${trade.type.name} ${Format.units(trade.units)} units @ ${Format.money(trade.pricePerUnit)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    Format.shortDate(trade.tradeDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (trade.type == TradeType.SELL && pnl != null) {
                    PnlText(amount = pnl, style = MaterialTheme.typography.bodyMedium)
                }
                if (trade.type == TradeType.BUY && trade.dividend > 0) {
                    Text(
                        "Dividend ${Format.signedMoney(trade.dividend)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit trade")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete trade")
            }
        }
    }
}
