package com.drl.padlock

import android.app.Application
import android.content.Context
import android.util.Log
import com.drl.padlock.listeners.LoginListener
import com.drl.padlock.listeners.Padlock
import com.drl.padlock.listeners.PadlockListListener
import com.drl.padlock.listeners.PadlockPairListener
import com.drl.padlock.listeners.PadlockSearchListener
import com.drl.padlock.utils.APP_KEY
import com.drl.padlock.utils.APP_SECRET
import com.drl.padlock.utils.CredentialsGenerator
import com.drl.padlock.utils.SmartSharedPreference
import com.thingclips.smart.android.ble.api.LeScanSetting
import com.thingclips.smart.android.ble.api.ScanType
import com.thingclips.smart.android.user.api.ILoginCallback
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.callback.IThingGetHomeListCallback
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLock
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IBleActivatorListener
import com.thingclips.smart.sdk.bean.BleActivatorBean
import com.thingclips.smart.sdk.bean.DeviceBean

internal const val HOME_NAME = "SMART_HOME"

class SmartLockSDK private constructor(app: Application, context: Context, config: String, showLogs: Boolean) {
    private var showLogs = false
    private var homeId = 0L
    private lateinit var lockDevice: IThingBleLock

    init {
        SmartSharedPreference.init(context)
        CredentialsGenerator().setup(config)
        ThingHomeSdk.init(
            app,
            SmartSharedPreference.getCredentials(APP_KEY),
            SmartSharedPreference.getCredentials(APP_SECRET)
        )
        ThingOptimusSdk.init(app)
    }

    fun login(
        countryCode: String,
        email: String,
        password: String,
        listener: LoginListener
    ) {
        if (ThingHomeSdk.getUserInstance().isLogin) {
            listener.onSuccess()
            return
        }

        ThingHomeSdk.getUserInstance().loginWithEmail(
            countryCode,
            email,
            password,
            object : ILoginCallback {
                override fun onSuccess(user: User?) {
                    setHome()
                    listener.onSuccess()
                }

                override fun onError(code: String?, error: String?) {
                    listener.onFailure(code, error.orEmpty())
                }
            }
        )
    }

    private fun setHome() {
        val callback = object : IThingGetHomeListCallback {
            override fun onSuccess(homeBeans: List<HomeBean>) {
                val b = homeBeans.firstOrNull { it.name.equals(HOME_NAME) }
                if (b == null) {
                    createHome()
                } else {
                    homeId = b.homeId
                }
            }

            override fun onError(errorCode: String?, error: String?) {
                log("$errorCode = $error")
            }
        }
        try {
            ThingHomeSdk.getHomeManagerInstance().queryHomeList(callback)
        } catch (e: Exception) {
            log(e.stackTraceToString())
        }
    }

    fun createHome() {
        val listener = object : IThingHomeResultCallback {
            override fun onSuccess(bean: HomeBean) {
                homeId = bean.homeId
            }

            override fun onError(errorCode: String?, errorMsg: String?) {
                log("$errorCode = $errorMsg")
            }
        }
        try {
            ThingHomeSdk.getHomeManagerInstance().createHome(
                HOME_NAME,
                0.0,
                0.0,
                "",
                arrayListOf(HOME_NAME),
                listener
            )
        } catch (e: Exception) {
            log(e.stackTraceToString())
        }
    }

    fun getAllDevices(listener: PadlockListListener) {
        val callBack = object : IThingHomeResultCallback {
            override fun onSuccess(bean: HomeBean?) {
                val beanList = bean?.deviceList ?: emptyList()
                val deviceList = beanList.map { bean ->
                    Padlock(
                        deviceBean = bean
                    )
                }.toList()
                listener.onSuccess(deviceList)
            }

            override fun onError(errorCode: String?, errorMsg: String?) {
                listener.onError(errorCode, errorMsg.orEmpty())
            }

        }
        try {
            ThingHomeSdk.newHomeInstance(homeId).getHomeDetail(callBack)
        } catch (e: Exception) {
            log(e.stackTraceToString())
        }
    }

    fun search(listener: PadlockSearchListener) {
        try {
            val scanSetting = LeScanSetting.Builder()
                .setTimeout(60_000)
                .addScanType(ScanType.SINGLE)
                .build()

            ThingHomeSdk.getBleOperator().startLeScan(scanSetting) { scanDeviceBean ->
                val padlock = Padlock(
                    scanDeviceBean = scanDeviceBean
                )
                listener.onSuccess(padlock)
            }
        } catch (e: Exception) {
            listener.onError(e)
        }
    }

    fun pair(padlock: Padlock, listener: PadlockPairListener) {
        if (padlock.scanDeviceBean == null) {
            listener.onError(errorCode = null, "Padlock details not found")
        }

        val callBack = object : IBleActivatorListener {
            override fun onSuccess(deviceBean: DeviceBean) {
                listener.onSuccess(Padlock(deviceBean))
            }

            override fun onFailure(code: Int, msg: String?, handle: Any?) {
                listener.onError(errorCode = code.toString(), errorMsg = msg.orEmpty())
            }
        }

        try {
            stopScan()
            val bleActivatorBean = BleActivatorBean(padlock.scanDeviceBean)
            ThingHomeSdk.getActivator().newBleActivator().startActivator(bleActivatorBean, callBack)

        } catch (e: Exception) {
            listener.onError(errorCode = null, errorMsg = e.stackTraceToString())
        }
    }

    fun getBatteryStatus(padlock: Padlock): String {
        return try {
            if (padlock.deviceBean == null) "NA"
            else padlock.deviceBean.getDps()["8"].toString()
        } catch (e: Exception) {
            log(e.stackTraceToString())
            "NA"
        }
    }

    fun connect(padlock: Padlock, onStatusChanged: (isConnected: Boolean) -> Unit) {
        val tuyaLockManager = ThingOptimusSdk.getManager(IThingLockManager::class.java)
        lockDevice = tuyaLockManager.getBleLock(padlock.devId)
        if (lockDevice.isBLEConnected) {
            onStatusChanged.invoke(true)
            return
        }

        lockDevice.connect { online -> onStatusChanged.invoke(online) }
    }

    fun lock() {
        connectDevice { it.lock() }
    }

    fun unlock() {
        connectDevice { it.unlock("1") }
    }

    private fun connectDevice(operation: (IThingBleLock) -> Unit) {
        if (!this::lockDevice.isInitialized) return
        if (lockDevice.isOnline) {
            operation.invoke(lockDevice)
        } else {
            lockDevice.connect { isOnline -> if (isOnline) operation.invoke(lockDevice) }
        }
    }

    fun stopScan() {
        ThingHomeSdk.getBleOperator().stopLeScan()
    }

    fun terminate() {
        try {
            ThingHomeSdk.onDestroy()
        } catch (e: Exception) {
            log(e.stackTraceToString())
        }
    }

    private fun log(message: String) {
        if (!showLogs) return
        Log.e("SmartSDK", "Something went wrong: $message")
    }

    companion object {

        @Volatile
        private var instance: SmartLockSDK? = null

        fun getInstance(
            app: Application,
            context: Context,
            config: String,
            showLogs: Boolean
        ): SmartLockSDK =
            instance ?: synchronized(this) {
                instance ?: SmartLockSDK(
                    app = app,
                    context = context,
                    config = config,
                    showLogs = showLogs
                ).also { instance = it }
            }
    }
}