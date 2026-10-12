package com.rhinos.app.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhinos.app.AppContainer
import com.rhinos.app.calculator.FifoEngine
import com.rhinos.app.calculator.StatsCalculator
import com.rhinos.app.ui.components.EmptyState
import com.rhinos.app.ui.components.Format
import com.rhinos.app.ui.components.PnlText
import com.rhinos.app.ui.components.SummaryCard
import com.rhinos.app.ui.theme.pnlColor
import com.rhinos.app.ui.daily.DailyPnlForm
import com.rhinos.app.ui.notes.NoteForm
import com.rhinos.app.ui.trades.TradeForm

/**
 * Overview: realized P&L (FIFO), dividends, recorded daily P&L, win rate, trade
 * count. Quick actions open the three recording flows directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenTrades: () -> Unit
) {
    val trades by container.tradeRepository.observeTrades()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val daily by container.dailyPnlRepository.observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
    Format.currencySymbol = settings.currencySymbol

    var showTradeForm by remember { mutableStateOf(false) }
    var showDailyForm by remember { mutableStateOf(false) }
    var showNoteForm by remember { mutableStateOf(false) }

    val analysis = remember(trades) { FifoEngine.analyze(trades) }
    val stats = remember(analysis) { StatsCalculator.winStats(analysis.sells.map { it.realizedPnl }) }
    val realizedPnl = remember(analysis) { analysis.sells.sumOf { it.realizedPnl } }
    val dividends = remember(trades) { trades.sumOf { it.dividend } }
    val dailyTotal = remember(daily) { daily.sumOf { it.amount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Rhinos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Your private investment journal",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        SummaryCard(
            label = "Realized P&L (completed sells)",
            value = Format.signedMoney(realizedPnl),
            valueColor = pnlColor(realizedPnl >= 0)
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                label = "Dividends",
                value = Format.signedMoney(dividends),
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Payments
            )
            SummaryCard(
                label = "Daily",
                value = Format.signedMoney(dailyTotal),
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.ShowChart
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                label = "Win Rate",
                value = Format.percent(stats.winRatePercent),
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Percent
            )
            SummaryCard(
                label = "Trades",
                value = Format.units(trades.size),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("Quick actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        Button(onClick = { showTradeForm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Record Trade")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showDailyForm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Record Daily P&L")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showNoteForm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Add Note")
        }

        if (trades.isEmpty() && daily.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            EmptyState(
                title = "Nothing recorded yet",
                message = "Record a trade above, or use the tabs below to explore."
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showTradeForm) {
        ModalBottomSheet(onDismissRequest = { showTradeForm = false }) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                TradeForm(container, editing = null, onDismiss = { showTradeForm = false })
            }
        }
    }
    if (showDailyForm) {
        ModalBottomSheet(onDismissRequest = { showDailyForm = false }) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                DailyPnlForm(container, editing = null, onDismiss = { showDailyForm = false })
            }
        }
    }
    if (showNoteForm) {
        ModalBottomSheet(onDismissRequest = { showNoteForm = false }) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                NoteForm(container, editing = null, onDismiss = { showNoteForm = false })
            }
        }
    }
}
