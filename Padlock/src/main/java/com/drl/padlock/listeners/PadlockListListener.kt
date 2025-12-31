package com.drl.padlock.listeners

import com.thingclips.smart.android.ble.api.ScanDeviceBean
import com.thingclips.smart.sdk.bean.DeviceBean

interface PadlockListListener {
    fun onSuccess(devices: List<Padlock>)
    fun onError(errorCode: String?, errorMsg: String)
}

data class Padlock(
    internal val deviceBean: DeviceBean? = null,
    internal val scanDeviceBean: ScanDeviceBean? = null
) {
    val uuid: String? get() = deviceBean?.getUuid() ?: scanDeviceBean?.getUuid()
    val devId: String? get() = deviceBean?.getDevId() ?: scanDeviceBean?.getUuid()
}