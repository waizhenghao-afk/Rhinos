package com.rhinos.app.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rhinos.app.MainActivity
import com.rhinos.app.R
import com.rhinos.app.RhinosApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires one day before a trade's expiry. Re-reads the trade from the database so
 * edited or deleted trades never produce a stale notification. Notification
 * permission denial or disabled tips silently no-op (the app never blocks on it).
 */
class ExpiryAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EXPIRY_TIP = "com.rhinos.app.action.EXPIRY_TIP"
        private const val NOTIFICATION_ID_BASE = 4000
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_EXPIRY_TIP) return
        val tradeId = intent.getLongExtra(ExpiryScheduler.EXTRA_TRADE_ID, -1L)
        if (tradeId <= 0) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val container = (appContext as RhinosApp).container
                val trade = container.tradeRepository.getTrade(tradeId) ?: return@launch
                val settings = container.settingsRepository.current()

                // Self-suppression checks: tips off, trade changed, already tipped.
                if (!settings.expiryTipsEnabled) return@launch
                if (trade.expiryDate == null) return@launch
                val today = java.time.LocalDate.now()
                val remindOn = trade.expiryDate!!.minusDays(1)
                if (today != remindOn) return@launch

                showNotification(appContext, trade.stockName, trade.marginType.label, trade.id)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        stockName: String,
        marginLabel: String,
        tradeId: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return // permission denied: rest of the app keeps working
        }

        val tapIntent = PendingIntent.getActivity(
            context,
            tradeId.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ExpiryScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_rhino)
            .setContentTitle(context.getString(R.string.app_name) + " Tip")
            .setContentText("Your $stockName $marginLabel period expires tomorrow.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your $stockName $marginLabel period expires tomorrow.")
            )
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + tradeId.toInt(), notification)
        } catch (e: SecurityException) {
            // permission revoked between check and notify: ignore
        }
    }
}
