package com.example.waterbottletracker.workers

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.example.waterbottletracker.data.WaterRepository
import com.example.waterbottletracker.widget.WaterWidget
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MidnightResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repo = WaterRepository(applicationContext)

        // Call the newly added function
        repo.resetDaily()

        // Update the widget
        WaterWidget().updateAll(applicationContext)

        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val currentDate = Calendar.getInstance()
            val dueDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(currentDate)) {
                    add(Calendar.HOUR_OF_DAY, 24)
                }
            }

            val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

            // Specify the explicit type for the builder
            val dailyWorkRequest = PeriodicWorkRequestBuilder<MidnightResetWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "MidnightReset",
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWorkRequest
            )
        }
    }
}