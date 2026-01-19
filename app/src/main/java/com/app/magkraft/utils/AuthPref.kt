package com.app.magkraft.utils

import android.content.Context

class AuthPref(context: Context) {

    private val prefs = context.getSharedPreferences(
        "auth_prefs",
        Context.MODE_PRIVATE
    )

    fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun get(key: String, default: String = ""): String {
        return prefs.getString(key, default) ?: default
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    private val prefsLocation = context.getSharedPreferences(
        "auth_prefs_location",
        Context.MODE_PRIVATE
    )

    fun putLocation(key: String, value: String) {
        prefsLocation.edit().putString(key, value).apply()
    }

    fun getLocation(key: String, default: String = ""): String {
        return prefsLocation.getString(key, default) ?: default
    }
}
