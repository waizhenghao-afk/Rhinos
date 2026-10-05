package com.rhinos.app.ui.reports

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.rhinos.app.calculator.ReportCalculator
import com.rhinos.app.ui.components.EmptyState
import com.rhinos.app.ui.components.Format
import com.rhinos.app.ui.theme.pnlColor
import java.time.LocalDate

/** Reports period filter chips. */
private enum class PeriodFilter { THIS_MONTH, PREV_MONTH, THIS_YEAR, PREV_YEAR, ALL }

/**
 * Monthly and yearly conclusions. Filter chips: This Month, Previous Month,
 * This Year, Previous Year, All.
 */
@Composable
fun ReportsScreen(container: AppContainer) {
    val trades by container.tradeRepository.observeTrades()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val daily by container.dailyPnlRepository.observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
    Format.currencySymbol = settings.currencySymbol

    val today = remember { LocalDate.now() }
    val prevMonth = remember { today.minusMonths(1) }
    val prevYear = remember { today.year - 1 }

    var filter by remember { mutableStateOf(PeriodFilter.THIS_MONTH) }

    val analysis = remember(trades) { FifoEngine.analyze(trades) }
    val sellPnlByTrade = remember(analysis) {
        analysis.sells.associate { it.tradeId to it.realizedPnl }
    }
    val reports = remember(trades, daily, sellPnlByTrade) {
        ReportCalculator.buildReports(trades, daily, sellPnlByTrade)
    }

    val visible = remember(reports, filter, today) {
        when (filter) {
            PeriodFilter.THIS_MONTH -> reports.filter {
                it.key.year == today.year && it.key.month == today.monthValue
            }
            PeriodFilter.PREV_MONTH -> reports.filter {
                it.key.year == prevMonth.year && it.key.month == prevMonth.monthValue
            }
            PeriodFilter.THIS_YEAR -> reports.filter { it.key.year == today.year }
            PeriodFilter.PREV_YEAR -> reports.filter { it.key.year == prevYear }
            PeriodFilter.ALL -> reports
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Reports", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Monthly and yearly conclusions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = filter == PeriodFilter.THIS_MONTH, onClick = { filter = PeriodFilter.THIS_MONTH }, label = { Text("This Month") })
                FilterChip(selected = filter == PeriodFilter.PREV_MONTH, onClick = { filter = PeriodFilter.PREV_MONTH }, label = { Text("Previous Month") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = filter == PeriodFilter.THIS_YEAR, onClick = { filter = PeriodFilter.THIS_YEAR }, label = { Text("This Year") })
                FilterChip(selected = filter == PeriodFilter.PREV_YEAR, onClick = { filter = PeriodFilter.PREV_YEAR }, label = { Text("Previous Year") })
                FilterChip(selected = filter == PeriodFilter.ALL, onClick = { filter = PeriodFilter.ALL }, label = { Text("All") })
            }
        }

        Spacer(Modifier.height(12.dp))

        if (reports.isEmpty()) {
            EmptyState(
                title = "No reports yet",
                message = "Record trades and daily P&L to generate monthly and yearly conclusions."
            )
        } else if (visible.isEmpty()) {
            EmptyState(title = "Nothing in this period", message = "Try another filter or record more activity.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visible, key = { "${it.key.year}-${it.key.month}" }) { report ->
                    ReportCard(report)
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun ReportCard(report: ReportCalculator.PeriodReport) {
    val title = if (report.isYear) Format.yearLabel(report.key.year)
    else Format.monthName(report.key.year, report.key.month!!)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            ReportRow("Realized P&L", Format.signedMoney(report.realizedPnl), pnlColor(report.realizedPnl >= 0))
            ReportRow("Daily P&L", Format.signedMoney(report.dailyPnl), pnlColor(report.dailyPnl >= 0))
            ReportRow("Dividends", Format.signedMoney(report.dividends), pnlColor(report.dividends >= 0))
            Spacer(Modifier.height(6.dp))
            ReportRow("Trades", Format.units(report.tradeCount), MaterialTheme.colorScheme.onSurface)
            ReportRow("Winning Trades", Format.units(report.winningTrades), pnlColor(true))
            ReportRow("Losing Trades", Format.units(report.losingTrades), pnlColor(false))
            ReportRow(
                "Win Rate",
                Format.percent(report.winRatePercent) + if (report.completedTrades > 0) "" else "",
                MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}
