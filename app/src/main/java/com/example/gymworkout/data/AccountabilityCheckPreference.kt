package com.example.gymworkout.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AccountabilityCheckPreference {
    private const val PREF_NAME = "accountability_check_prefs"
    private const val KEY_TIME_PREFIX = "time_"
    const val DEFAULT_TIME = "20:00"

    private val _partnerTimes = MutableStateFlow<Map<String, String>>(emptyMap())
    val partnerTimes: StateFlow<Map<String, String>> = _partnerTimes

    fun init(context: Context) {
        _partnerTimes.value = loadAll(context)
    }

    fun getTimeForPartner(context: Context, partnershipId: String): String {
        if (partnershipId.isEmpty()) return DEFAULT_TIME
        return prefs(context).getString(KEY_TIME_PREFIX + partnershipId, DEFAULT_TIME) ?: DEFAULT_TIME
    }

    fun setTimeForPartner(context: Context, partnershipId: String, time: String) {
        if (partnershipId.isEmpty()) return
        prefs(context).edit().putString(KEY_TIME_PREFIX + partnershipId, time).apply()
        _partnerTimes.value = loadAll(context)
    }

    fun ensureTimeForPartner(context: Context, partnershipId: String): String {
        if (partnershipId.isEmpty()) return DEFAULT_TIME
        val p = prefs(context)
        val existing = p.getString(KEY_TIME_PREFIX + partnershipId, null)
        if (existing != null) return existing
        p.edit().putString(KEY_TIME_PREFIX + partnershipId, DEFAULT_TIME).apply()
        _partnerTimes.value = loadAll(context)
        return DEFAULT_TIME
    }

    fun clearPartner(context: Context, partnershipId: String) {
        if (partnershipId.isEmpty()) return
        prefs(context).edit().remove(KEY_TIME_PREFIX + partnershipId).apply()
        _partnerTimes.value = loadAll(context)
    }

    fun getAllPartnerTimes(context: Context): Map<String, String> = loadAll(context)

    private fun loadAll(context: Context): Map<String, String> {
        val all = prefs(context).all
        val out = HashMap<String, String>()
        for ((k, v) in all) {
            if (k.startsWith(KEY_TIME_PREFIX) && v is String) {
                out[k.removePrefix(KEY_TIME_PREFIX)] = v
            }
        }
        return out
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
