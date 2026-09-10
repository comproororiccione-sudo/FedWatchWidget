package it.goldsolution.fedwatchfree.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import it.goldsolution.fedwatchfree.data.FedFundsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FedWatchWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val r = FedFundsRepository.loadNextMeeting()
            WidgetStore.save(applicationContext, r)
            FedWatchWidgetProvider.updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            WidgetStore.saveError(applicationContext, e.message ?: "Errore dati")
            FedWatchWidgetProvider.updateAll(applicationContext)
            Result.retry()
        }
    }
}
