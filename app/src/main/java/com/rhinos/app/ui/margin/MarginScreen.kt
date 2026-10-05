package com.rhinos.app.ui.margin

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhinos.app.AppContainer
import com.rhinos.app.calculator.ExpiryCalculator
import com.rhinos.app.data.model.Trade
import com.rhinos.app.ui.components.EmptyState
import com.rhinos.app.ui.components.Format
import java.time.LocalDate

/**
 * Margin / Expiry page. Shows every trade with a margin period, its computed
 * expiry date and a clear status label. Trades with no expiry (permanent) are
 * shown separately at the bottom so active expiries stay on top.
 */
@Composable
fun MarginScreen(container: AppContainer) {
    val trades by container.tradeRepository.observeTrades()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
    Format.currencySymbol = settings.currencySymbol

    val today = remember { LocalDate.now() }

    val withExpiry = remember(trades, today) {
        trades.filter { it.expiryDate != null }
            .sortedWith(compareBy({ it.expiryDate }, { it.stockName }))
    }
    val permanent = remember(trades) {
        trades.filter { it.expiryDate == null }.sortedBy { it.stockName }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Margin / Expiry", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Personal reminders, not official settlement dates",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (trades.isEmpty()) {
            EmptyState(title = "No active expiry periods", message = "Trades with T+3 or T+10 margin will appear here.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(
                        "Expiring",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(withExpiry, key = { "e" + it.id }) { trade ->
                    MarginCard(trade, today)
                }
                if (permanent.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Permanent (no expiry)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(permanent, key = { "p" + it.id }) { trade ->
                        MarginCard(trade, today)
                    }
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun MarginCard(trade: Trade, today: LocalDate) {
    val status = remember(trade.id, today) { ExpiryCalculator.status(trade.expiryDate, today) }
    val days = remember(trade.id, today) { ExpiryCalculator.daysRemaining(trade.expiryDate, today) }

    val statusLabel = when (status) {
        ExpiryCalculator.Status.Active -> "Active"
        is ExpiryCalculator.Status.ExpiresInDays -> "Expires in ${status.days} days"
        ExpiryCalculator.Status.ExpiresTomorrow -> "Expires Tomorrow"
        ExpiryCalculator.Status.ExpiresToday -> "Expires Today"
        ExpiryCalculator.Status.Expired -> "Expired"
        ExpiryCalculator.Status.NoExpiry -> "No Expiry"
    }
    val statusColor = when (status) {
        ExpiryCalculator.Status.Expired -> MaterialTheme.colorScheme.error
        ExpiryCalculator.Status.ExpiresToday, ExpiryCalculator.Status.ExpiresTomorrow ->
            MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(trade.stockName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    trade.marginType.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (trade.expiryDate == null) "Permanent"
                else "Expires: ${Format.shortDate(trade.expiryDate!!)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Trade date: ${Format.shortDate(trade.tradeDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Status: $statusLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
