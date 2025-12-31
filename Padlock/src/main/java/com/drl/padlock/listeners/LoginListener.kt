package com.drl.padlock.listeners

interface LoginListener {
    fun onSuccess(): Unit
    fun onFailure(code: String?, error: String): Unit
}