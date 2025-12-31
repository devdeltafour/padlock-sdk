package com.drl.padlock.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

internal const val APP_KEY = "smart_app_key"
internal const val APP_SECRET = "smart_app_secret"
internal const val APP_COUNTRY_CODE = "country_code"
internal const val APP_CAPTCHA = "smart_captcha"

internal object SmartSharedPreference {
    private lateinit var sharedPreference: SharedPreferences

    fun init(context: Context) {
        if (this::sharedPreference.isInitialized) return
        sharedPreference = context.getSharedPreferences(
            "smart_secret_preference", Context.MODE_PRIVATE
        )
    }

    fun saveCredentials(key: String, value: String) {
        sharedPreference.edit { putString(key, value) }
    }

    fun getCredentials(key: String): String {
        return sharedPreference.getString(key, "").orEmpty()
    }
}