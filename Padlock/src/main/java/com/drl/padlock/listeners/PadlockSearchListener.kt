package com.drl.padlock.listeners

interface PadlockSearchListener {
    fun onSuccess(padlock: Padlock)
    fun onError(error: Exception)
}