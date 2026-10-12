package com.rhinos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rhinos.app.ui.theme.pnlColor

/** Card with label on top and a big value underneath — the app's basic summary unit. */
@Composable
fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    icon: ImageVector? = null
) {
    Card(
        modifier = modifier.semantics { contentDescription = "$label: $value" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(Modifier.height(0.dp))
                    Spacer(Modifier.padding(start = 6.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}

/** Amount text that is legible in both themes and always carries an explicit sign. */
@Composable
fun PnlText(
    amount: Double,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    forcePlus: Boolean = false
) {
    val positive = amount >= 0
    val text = if (forcePlus && amount >= 0) Format.signedMoney(amount)
    else if (amount >= 0) Format.signedMoney(amount) else Format.signedMoney(amount)
    Text(
        text = text,
        color = if (positive) pnlColor(true) else pnlColor(false),
        style = style,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.semantics {
            contentDescription = (if (positive) "profit " else "loss ") + Format.signedMoney(amount)
        }
    )
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Insights,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.height(48.dp)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/** Simple up/down indicator used next to buy/sell rows. */
@Composable
fun DirectionIcon(type: com.rhinos.app.data.model.TradeType, modifier: Modifier = Modifier) {
    val (icon, tint, label) = when (type) {
        com.rhinos.app.data.model.TradeType.BUY ->
            Triple(Icons.Outlined.TrendingUp, MaterialTheme.colorScheme.primary, "Buy")
        com.rhinos.app.data.model.TradeType.SELL ->
            Triple(Icons.Outlined.TrendingDown, pnlColor(false), "Sell")
    }
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint,
        modifier = modifier
    )
}
