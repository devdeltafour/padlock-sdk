package com.drl.padlock.utils

import com.google.gson.annotations.SerializedName

data class TuyaCredentials(
    @SerializedName("tuyaAppKey") val tuyaAppKey: String,
    @SerializedName("tuyaSecret") val tuyaSecret: String,
    @SerializedName("countryCode") val countryCode: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("captchaKey") val captchaKey: String,
)