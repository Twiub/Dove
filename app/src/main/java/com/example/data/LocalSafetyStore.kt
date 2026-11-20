package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.doveSafetyDataStore by preferencesDataStore(name = "dove_safety_prefs")

class LocalSafetyStore(private val context: Context) {
    private val under18BlockUntilKey = longPreferencesKey("under_18_block_until_ms")

    val under18BlockedUntilMs: Flow<Long> = context.doveSafetyDataStore.data.map { prefs ->
        prefs[under18BlockUntilKey] ?: 0L
    }

    suspend fun recordUnder18Block(days: Long = 30L) {
        val until = System.currentTimeMillis() + days * 86_400_000L
        context.doveSafetyDataStore.edit { prefs ->
            prefs[under18BlockUntilKey] = until
        }
    }
}
