package com.drl.padlock.utils

import com.google.gson.Gson
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal class CredentialsGenerator() {

    fun setup(config: String){
        if(SmartSharedPreference.getCredentials(APP_KEY).isNotEmpty()) return

        val credentials = credentialsFromConfig(config)
        saveCredentials(credentials)
    }
    private fun credentialsFromConfig(config: String) = generate(config)

    private fun saveCredentials(credentials: String) {
        val tuyaCredentials = Gson().fromJson(credentials, TuyaCredentials::class.java)
        SmartSharedPreference.saveCredentials(APP_KEY, tuyaCredentials.tuyaAppKey)
        SmartSharedPreference.saveCredentials(APP_SECRET, tuyaCredentials.tuyaSecret)
        SmartSharedPreference.saveCredentials(APP_COUNTRY_CODE, tuyaCredentials.countryCode)
        SmartSharedPreference.saveCredentials(APP_CAPTCHA, tuyaCredentials.captchaKey)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generate(value: String): String {
        return String(Base64.decode(value.toByteArray(), 0, value.length))
    }
}
