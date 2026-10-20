package com.example.waterbottletracker.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.dataStore by preferencesDataStore(name = "water_settings")

/** Theme selection was removed - the app now always uses this single hydration blue. */
const val APP_BLUE: Long = 0xFF2F8CFF

data class WaterState(
    val mlDrank: Int = 0,
    val dailyGoalMl: Int = 5000,
    // The most recently logged amount. Drives the widget's "+" button and which
    // Quick Add chip shows as selected.
    val lastAmountMl: Int = 250,
    val bottleSizeMl: Int = 500
)

/**
 * A single day's snapshot in the history/heatmap.
 * Goal is captured at the time the day was recorded, so past days stay accurate
 * even if the user changes their daily goal later.
 */
data class DayRecord(
    val date: LocalDate,
    val mlDrank: Int,
    val goalMl: Int
) {
    /** Uncapped ratio - can exceed 1f if the user drank past their goal. */
    val rawProgress: Float get() = if (goalMl == 0) 0f else mlDrank.toFloat() / goalMl

    /** Clamped 0..1, useful for progress bars / heatmap fill. */
    val progress: Float get() = rawProgress.coerceIn(0f, 1f)
}

class WaterRepository(private val context: Context) {
    private object Keys {
        val ML_DRANK = intPreferencesKey("ml_drank")
        val GOAL_ML = intPreferencesKey("goal_ml")
        val BOTTLE_SIZE_ML = intPreferencesKey("bottle_size_ml")
        val LAST_AMOUNT_ML = intPreferencesKey("last_amount_ml")
        val LAST_DATE = longPreferencesKey("last_date")
    }

    private val HISTORY_PREFIX = "history_"
    private fun historyKey(epochDay: Long) = stringPreferencesKey("$HISTORY_PREFIX$epochDay")

    val waterStateFlow: Flow<WaterState> = context.dataStore.data.map { prefs ->
        val lastDate = prefs[Keys.LAST_DATE] ?: 0L
        val today = LocalDate.now().toEpochDay()
        val isNewDay = today > lastDate
        val currentMl = if (isNewDay) 0 else (prefs[Keys.ML_DRANK] ?: 0)

        WaterState(
            mlDrank = currentMl,
            dailyGoalMl = prefs[Keys.GOAL_ML] ?: 5000,
            lastAmountMl = prefs[Keys.LAST_AMOUNT_ML] ?: 250,
            bottleSizeMl = prefs[Keys.BOTTLE_SIZE_ML] ?: 500
        )
    }

    /** All recorded days, most recent first. Includes today (kept live as amounts are logged). */
    val historyFlow: Flow<List<DayRecord>> = context.dataStore.data.map { prefs ->
        prefs.asMap().entries.mapNotNull { (key, value) ->
            val name = key.name
            if (!name.startsWith(HISTORY_PREFIX) || value !is String) return@mapNotNull null
            val epochDay = name.removePrefix(HISTORY_PREFIX).toLongOrNull() ?: return@mapNotNull null
            val parts = value.split(",")
            if (parts.size != 2) return@mapNotNull null
            val ml = parts[0].toIntOrNull() ?: return@mapNotNull null
            val goal = parts[1].toIntOrNull() ?: return@mapNotNull null
            DayRecord(LocalDate.ofEpochDay(epochDay), ml, goal)
        }.sortedByDescending { it.date }
    }

    /** Logs a Quick Add amount (or a custom one) for today. */
    suspend fun logAmount(amountMl: Int) {
        context.dataStore.edit { prefs ->
            val today = LocalDate.now().toEpochDay()
            val lastDate = prefs[Keys.LAST_DATE] ?: today
            val goal = prefs[Keys.GOAL_ML] ?: 5000

            // If the day rolled over since the last write, archive the day that just ended
            // before resetting the running total.
            if (today > lastDate) {
                val previousMl = prefs[Keys.ML_DRANK] ?: 0
                if (previousMl > 0) {
                    prefs[historyKey(lastDate)] = "$previousMl,$goal"
                }
            }

            val current = if (today > lastDate) 0 else (prefs[Keys.ML_DRANK] ?: 0)
            val updated = current + amountMl
            prefs[Keys.ML_DRANK] = updated
            prefs[Keys.LAST_DATE] = today
            prefs[Keys.LAST_AMOUNT_ML] = amountMl
            prefs[historyKey(today)] = "$updated,$goal"
        }
    }

    /** Undoes the last logged amount (used by the widget's minus button / "Undo last"). */
    suspend fun undoLastAmount(amountMl: Int) {
        context.dataStore.edit { prefs ->
            val today = LocalDate.now().toEpochDay()
            val current = prefs[Keys.ML_DRANK] ?: 0
            if (current > 0) {
                val updated = (current - amountMl).coerceAtLeast(0)
                prefs[Keys.ML_DRANK] = updated
                val goal = prefs[Keys.GOAL_ML] ?: 5000
                prefs[historyKey(today)] = "$updated,$goal"
            }
        }
    }

    suspend fun updateSettings(goalMl: Int, bottleSizeMl: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOAL_ML] = goalMl
            prefs[Keys.BOTTLE_SIZE_ML] = bottleSizeMl
        }
    }
    suspend fun resetDaily() {
        context.dataStore.edit { prefs ->
            // Safety net: make sure the outgoing day is archived even if no log call
            // happened to write it live (e.g. app wasn't opened right before midnight).
            val lastDate = prefs[Keys.LAST_DATE] ?: LocalDate.now().toEpochDay()
            val ml = prefs[Keys.ML_DRANK] ?: 0
            val goal = prefs[Keys.GOAL_ML] ?: 5000
            if (ml > 0) {
                prefs[historyKey(lastDate)] = "$ml,$goal"
            }

            prefs[Keys.ML_DRANK] = 0
            prefs[Keys.LAST_DATE] = LocalDate.now().toEpochDay()
        }
    }
}