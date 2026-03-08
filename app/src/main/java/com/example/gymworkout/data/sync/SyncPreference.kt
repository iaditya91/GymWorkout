package com.example.gymworkout.data.sync

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SyncPreference {
    private const val PREF_NAME = "sync_prefs"
    private const val KEY_LAST_SYNC = "last_sync_time"
    private const val KEY_ACCOUNT_EMAIL = "google_account_email"
    private const val KEY_ACCOUNT_NAME = "google_account_name"

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime

    private val _accountEmail = MutableStateFlow<String?>(null)
    val accountEmail: StateFlow<String?> = _accountEmail

    private val _accountName = MutableStateFlow<String?>(null)
    val accountName: StateFlow<String?> = _accountName

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _lastSyncTime.value = prefs.getLong(KEY_LAST_SYNC, 0L)
        _accountEmail.value = prefs.getString(KEY_ACCOUNT_EMAIL, null)
        _accountName.value = prefs.getString(KEY_ACCOUNT_NAME, null)
    }

    fun setLastSyncTime(context: Context, time: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_SYNC, time).apply()
        _lastSyncTime.value = time
    }

    fun setAccount(context: Context, email: String?, name: String?) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
        if (email != null) prefs.putString(KEY_ACCOUNT_EMAIL, email)
        else prefs.remove(KEY_ACCOUNT_EMAIL)
        if (name != null) prefs.putString(KEY_ACCOUNT_NAME, name)
        else prefs.remove(KEY_ACCOUNT_NAME)
        prefs.apply()
        _accountEmail.value = email
        _accountName.value = name
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
        _lastSyncTime.value = 0L
        _accountEmail.value = null
        _accountName.value = null
    }
}
