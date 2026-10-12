package com.rhinos.app.ui.trades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
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
import java.time.LocalDate
import java.time.YearMonth

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
    var showCalendar by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Trade?>(null) }
    var deleting by remember { mutableStateOf<Trade?>(null) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }

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

    val monthlyPnlByDate = remember(trades, calendarMonth) {
        val monthTrades = trades.filter {
            it.tradeDate.year == calendarMonth.year &&
                it.tradeDate.monthValue == calendarMonth.monthValue
        }
        monthTrades.groupBy { it.tradeDate }
            .mapValues { (_, dayTrades) ->
                dayTrades.sumOf { trade -> pnlByTrade[trade.id] ?: 0.0 }
            }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Trades", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { editing = null; showForm = true }, modifier = Modifier.weight(1f)) {
                Text("+ Record Trade")
            }
            OutlinedButton(
                onClick = { showCalendar = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Calendar")
            }
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

    if (showCalendar) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(0.98f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showCalendar = false },
            title = { Text("${calendarMonth.month.name.lowercase().replaceFirstChar { it.titlecase() }} ${calendarMonth.year}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        dayNames.forEach { day ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(day, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    val firstDay = calendarMonth.atDay(1)
                    val offset = firstDay.dayOfWeek.value % 7
                    val totalDays = calendarMonth.lengthOfMonth()
                    val cells = mutableListOf<LocalDate?>()
                    repeat(offset) { cells.add(null) }
                    for (day in 1..totalDays) {
                        cells.add(LocalDate.of(calendarMonth.year, calendarMonth.monthValue, day))
                    }
                    while (cells.size % 7 != 0) {
                        cells.add(null)
                    }

                    cells.chunked(7).forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            week.forEach { value ->
                                val isFilled = value != null
                                val pnl = if (value != null) {
                                    monthlyPnlByDate[value] ?: 0.0
                                } else 0.0
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .background(
                                            if (!isFilled) Color.Transparent else if (pnl >= 0.0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isFilled) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                value.dayOfMonth.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                if (pnl == 0.0) "-" else Format.signedMoney(pnl)
                                                    .replace(Format.currencySymbol, "")
                                                    .replace(" ", "")
                                                    .replace(",", ""),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (pnl >= 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { calendarMonth = calendarMonth.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Previous month")
                    }
                    TextButton(onClick = { showCalendar = false }) {
                        Text("Close")
                    }
                    IconButton(onClick = { calendarMonth = calendarMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Next month")
                    }
                }
            }
        )
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
