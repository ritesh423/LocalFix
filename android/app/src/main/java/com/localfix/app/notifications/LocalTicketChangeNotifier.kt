package com.localfix.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.localfix.app.MainActivity
import com.localfix.app.R
import com.localfix.app.data.remote.TicketResponse
import org.json.JSONObject

/** Alerts only when a successful refresh finds a change since this device's last refresh. */
class LocalTicketChangeNotifier(
    private val context: Context,
    private val currentFirebaseUid: () -> String?,
) {
    private val snapshots = context.getSharedPreferences(
        "localfix_ticket_change_snapshots",
        Context.MODE_PRIVATE,
    )

    fun onResidentTickets(tickets: List<TicketResponse>) = observe("resident", tickets)

    fun onManagerTickets(tickets: List<TicketResponse>) = observe("manager", tickets)

    fun onWorkerTickets(tickets: List<TicketResponse>) = observe("worker", tickets)

    private fun observe(role: String, tickets: List<TicketResponse>) {
        val uid = currentFirebaseUid() ?: return
        val key = "$uid:$role"
        val previousText = snapshots.getString(key, null)
        val previous = previousText?.let { runCatching { JSONObject(it) }.getOrNull() }
        val current = JSONObject()
        tickets.forEach { current.put(it.id, it.status) }
        snapshots.edit().putString(key, current.toString()).apply()

        // The first successful load establishes a baseline; it is not a new event.
        if (previous == null) return
        val changed = tickets.filter { ticket ->
            when {
                previous.has(ticket.id) -> previous.optString(ticket.id) != ticket.status
                role == "manager" || role == "worker" -> true
                else -> false
            }
        }
        if (changed.isEmpty()) return
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val title = if (changed.size == 1) {
            when (role) {
                "manager" -> "Manager queue updated"
                "worker" -> "Your work queue updated"
                else -> "Your request was updated"
            }
        } else {
            "${changed.size} maintenance requests updated"
        }
        val body = if (changed.size == 1) {
            "${changed.first().title} · ${changed.first().status.replace('_', ' ')}"
        } else {
            "Open LocalFix to see what changed."
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(
            context,
            NotificationChannels.UPDATES_CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(
            key.hashCode(),
            notification,
        )
    }
}
