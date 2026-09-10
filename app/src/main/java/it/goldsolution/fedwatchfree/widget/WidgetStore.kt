package it.goldsolution.fedwatchfree.widget

import android.content.Context
import it.goldsolution.fedwatchfree.data.FedFundsRepository

object WidgetStore {
    private const val PREF = "fedwatch_cache"
    fun save(c: Context, r: FedFundsRepository.Result) {
        val e = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("date", r.meetingDate.toString())
            .putString("a", r.outcomeA).putFloat("pa", r.probabilityA.toFloat())
            .putString("b", r.outcomeB).putFloat("pb", r.probabilityB.toFloat())
            .putString("source", r.source)
            .putString("kind", r.dataKind.name)
            .putLong("updated", System.currentTimeMillis())
            .remove("error")

        r.meetingFuture?.let { e.putFloat("mkt", it.toFloat()) } ?: e.remove("mkt")
        r.anchorFuture?.let { e.putFloat("anchor", it.toFloat()) } ?: e.remove("anchor")
        r.expectedChangeBps?.let { e.putFloat("delta", it.toFloat()) } ?: e.remove("delta")
        e.apply()
    }

    fun saveError(c: Context, error: String) =
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("error", error).apply()

    fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}
