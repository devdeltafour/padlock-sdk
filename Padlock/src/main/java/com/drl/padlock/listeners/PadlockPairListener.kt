package com.drl.padlock.listeners

interface PadlockPairListener {
    fun onSuccess(deviceBean: Padlock)
    fun onError(errorCode: String?, errorMsg: String)
}