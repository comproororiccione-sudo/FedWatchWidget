package it.goldsolution.fedwatchfree.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.*
import it.goldsolution.fedwatchfree.R
import it.goldsolution.fedwatchfree.data.FedFundsRepository
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class FedWatchWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) { schedule(context); refreshNow(context) }
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { updateAll(context); refreshNow(context) }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refreshNow(context)
    }

    companion object {
        const val ACTION_REFRESH = "it.goldsolution.fedwatchfree.REFRESH"

        fun schedule(c: Context) {
            val req = PeriodicWorkRequestBuilder<FedWatchWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(c).enqueueUniquePeriodicWork("fedwatch_periodic", ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun refreshNow(c: Context) {
            WorkManager.getInstance(c).enqueueUniqueWork(
                "fedwatch_now", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<FedWatchWorker>().setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            )
        }

        fun updateAll(c: Context) {
            val manager = AppWidgetManager.getInstance(c)
            val ids = manager.getAppWidgetIds(ComponentName(c, FedWatchWidgetProvider::class.java))
            val p = WidgetStore.prefs(c)
            val a = p.getString("a", null)
            val err = p.getString("error", null)
            val views = RemoteViews(c.packageName, R.layout.widget_fedwatch)

            if (a == null) {
                views.setTextViewText(R.id.primary, if (err == null) "Caricamento…" else "Dato non disponibile")
                views.setTextViewText(R.id.secondary, err?.take(80) ?: "")
            } else {
                val pa = p.getFloat("pa", 0f)
                val b = p.getString("b", "") ?: ""
                val pb = p.getFloat("pb", 0f)
                views.setTextViewText(R.id.primary, "$a  ${"%.1f".format(pa)}%")
                views.setTextViewText(R.id.secondary, "$b  ${"%.1f".format(pb)}%")
                val kind = p.getString("kind", "")
                val source = p.getString("source", "fonte gratuita") ?: "fonte gratuita"
                if (kind == FedFundsRepository.DataKind.CME_PUBLIC.name) {
                    views.setTextViewText(R.id.rates, "DATO CME PUBBLICO • coincide con la tabella FedWatch")
                } else {
                    views.setTextViewText(R.id.rates, "⚠ STIMA FALLBACK • ZQ ${"%.3f".format(p.getFloat("mkt",0f))} / ${"%.3f".format(p.getFloat("anchor",0f))} • Δ ${"%+.1f".format(p.getFloat("delta",0f))} bp")
                }
                val t = p.getLong("updated", 0L)
                views.setTextViewText(R.id.updated, "Aggiornato ${SimpleDateFormat("HH:mm", Locale.ITALY).format(Date(t))} • $source")
            }

            val refreshIntent = Intent(c, FedWatchWidgetProvider::class.java).setAction(ACTION_REFRESH)
            val pi = PendingIntent.getBroadcast(c, 1, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.refresh, pi)
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }
}
