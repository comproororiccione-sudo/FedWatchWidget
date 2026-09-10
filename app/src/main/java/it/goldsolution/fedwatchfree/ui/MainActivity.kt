package it.goldsolution.fedwatchfree.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import it.goldsolution.fedwatchfree.widget.FedWatchWidgetProvider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad,pad,pad,pad) }
        root.addView(TextView(this).apply { text = "Fed Funds Watch — gratuito"; textSize = 24f })
        root.addView(TextView(this).apply {
            text = "Widget Android basato sui futures 30-Day Fed Funds (ZQ) e sulla metodologia pubblicata da CME. Non usa l’API FedWatch a pagamento.\n\nAggiungi il widget dalla schermata Home. Tocca ↻ per aggiornare. Android consente gli aggiornamenti periodici affidabili con WorkManager; qui sono impostati ogni 30 minuti.\n\nFonte prezzi iniziale: tabella pubblica TradingView. Il parser è separato dal widget per poter cambiare sorgente facilmente se il sito modifica il formato."
            textSize = 16f; setPadding(0,pad,0,0)
        })
        setContentView(root)
        FedWatchWidgetProvider.schedule(this)
        FedWatchWidgetProvider.refreshNow(this)
    }
}
