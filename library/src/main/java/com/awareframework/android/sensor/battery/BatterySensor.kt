package com.awareframework.android.sensor.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.awareframework.android.core.AwareSensor
import com.awareframework.android.core.db.Engine
import com.awareframework.android.core.db.model.DbSyncConfig
import com.awareframework.android.core.model.AwareData
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_BATTERY_CHANGED
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_BATTERY_CHARGING
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_BATTERY_CHARGING_AC
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_BATTERY_CHARGING_USB
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_BATTERY_DISCHARGING
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_BATTERY_FULL
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_BATTERY_LABEL
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_BATTERY_LOW
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_BATTERY_START
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_BATTERY_STOP
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_BATTERY_SYNC
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_PHONE_REBOOT
import com.awareframework.android.sensor.battery.Battery.Companion.ACTION_AWARE_PHONE_SHUTDOWN
import com.awareframework.android.sensor.battery.Battery.Companion.EXTRA_AWARE_BATTERY_LABEL
import com.awareframework.android.sensor.battery.Battery.Companion.STATUS_PHONE_REBOOT
import com.awareframework.android.sensor.battery.Battery.Companion.STATUS_PHONE_SHUTDOWN
import com.awareframework.android.sensor.battery.model.BatteryCharge
import com.awareframework.android.sensor.battery.model.BatteryData
import com.awareframework.android.sensor.battery.model.BatteryDischarge
import java.util.*

/**
 * Service that logs power related events (battery and shutdown/reboot)
 * - Battery changed
 * - Battery charging
 * - Battery discharging
 * - Battery full
 * - Battery low
 * - Phone shutdown
 * - Phone reboot
 *
 * @author  sercant
 * @date 23/04/2018
 */
class BatterySensor internal constructor() : AwareSensor() {

    companion object {
        /**
         * Logging tag "BatterySensor"
         */
        const val TAG = "BatterySensor"

        val CONFIG = Battery.BatteryConfig()

        var instance: BatterySensor? = null
    }

    private val batteryFilterIntentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_BATTERY_CHANGED)
        addAction(Intent.ACTION_BATTERY_LOW)
        addAction(Intent.ACTION_SHUTDOWN)
        addAction(Intent.ACTION_REBOOT)
        addAction(Intent.ACTION_POWER_CONNECTED)
        addAction(Intent.ACTION_POWER_DISCONNECTED)
    }

    /**
     * BroadcastReceiver for Battery module
     * - ACTION_BATTERY_CHANGED: battery values changed
     * - ACTION_BATTERY_PLUGGED_AC: user is charging via AC
     * - ACTION_BATTERY_PLUGGED_USB: user is charging via USB
     * - ACTION_BATTERY_STATUS_FULL: battery finished charging
     * - ACTION_POWER_CONNECTED: power is connected
     * - ACTION_POWER_DISCONNECTED: power is disconnected
     * - ACTION_BATTERY_LOW: battery is running low (15% by Android OS)
     * - ACTION_SHUTDOWN: phone is about to shut down
     * - ACTION_REBOOT: phone is about to reboot
     */
    private val batteryBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_BATTERY_CHANGED -> {
                    val extras = intent.extras
                    extras ?: return
                    onBatteryChanged(extras)
                }
                ACTION_POWER_CONNECTED -> onPowerConnected()
                ACTION_POWER_DISCONNECTED -> onPowerDisconnected()
                ACTION_BATTERY_LOW -> onBatteryLow()
                ACTION_SHUTDOWN -> onShutDown()
                ACTION_REBOOT -> onReboot()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        dbEngine = Engine.Builder(applicationContext)
                .setType(CONFIG.dbType)
                .setPath(CONFIG.dbPath)
                .setHost(CONFIG.dbHost)
                .setEncryptionKey(CONFIG.dbEncryptionKey)
                .build()

        registerReceiver(batteryBroadcastReceiver, batteryFilterIntentFilter)
        registerReceiver(batterySpecificBroadcastReceiver, IntentFilter().apply {
            addAction(ACTION_AWARE_BATTERY_SYNC)
            addAction(ACTION_AWARE_BATTERY_LABEL)
        })

        logd("Battery service created!")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        logd("Battery service active...")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(batteryBroadcastReceiver)
        unregisterReceiver(batterySpecificBroadcastReceiver)

        dbEngine?.close()
        dbEngine = null

        instance = null

        logd("Battery service terminated...")
    }

    inner class BatteryServiceBinder : Binder() {
        fun getService(): BatterySensor {
            return instance ?: BatterySensor()
        }
    }

    private val binder: BatteryServiceBinder = BatteryServiceBinder()

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onSync(intent: Intent?) {
        dbEngine?.startSync(BatteryData.TABLE_NAME, DbSyncConfig(keepLastData = true))
        dbEngine?.startSync(BatteryCharge.TABLE_NAME)
        dbEngine?.startSync(BatteryDischarge.TABLE_NAME)
    }

    private fun onBatteryChanged(extras: Bundle) {
        val callback: (AwareData?) -> Unit = {
            var changed = true
            it?.withData<BatteryData> { data ->
                changed = extras.getInt(BatteryManager.EXTRA_LEVEL) != data.level ||
                        extras.getInt(BatteryManager.EXTRA_PLUGGED) != data.adaptor ||
                        extras.getInt(BatteryManager.EXTRA_STATUS) != data.status
            }

            if (changed) {
                val newData = BatteryData().apply {
                    timestamp = System.currentTimeMillis()
                    timezone = TimeZone.getDefault().rawOffset
                    deviceId = CONFIG.deviceId
                    label = CONFIG.label
                    status = extras.getInt(BatteryManager.EXTRA_STATUS)
                    level = extras.getInt(BatteryManager.EXTRA_LEVEL)
                    scale = extras.getInt(BatteryManager.EXTRA_SCALE)
                    voltage = extras.getInt(BatteryManager.EXTRA_VOLTAGE)
                    temperature = extras.getInt(BatteryManager.EXTRA_TEMPERATURE) / 10
                    adaptor = extras.getInt(BatteryManager.EXTRA_PLUGGED)
                    health = extras.getInt(BatteryManager.EXTRA_HEALTH)
                    technology = extras.getString(BatteryManager.EXTRA_TECHNOLOGY)
                }

                dbEngine?.save(newData, BatteryData.TABLE_NAME)
                CONFIG.sensorObserver?.onBatteryChanged(newData)

                if (newData.adaptor == BatteryManager.BATTERY_PLUGGED_AC) {
                    logd(ACTION_AWARE_BATTERY_CHARGING_AC)
                    applicationContext.sendBroadcast(Intent(ACTION_AWARE_BATTERY_CHARGING_AC))
                }

                if (newData.adaptor == BatteryManager.BATTERY_PLUGGED_USB) {
                    logd(ACTION_AWARE_BATTERY_CHARGING_USB)
                    applicationContext.sendBroadcast(Intent(ACTION_AWARE_BATTERY_CHARGING_USB))
                }

                if (newData.status == BatteryManager.BATTERY_STATUS_FULL) {
                    logd(ACTION_AWARE_BATTERY_FULL)
                    applicationContext.sendBroadcast(Intent(ACTION_AWARE_BATTERY_FULL))
                }

                logd(ACTION_AWARE_BATTERY_CHANGED)
                applicationContext.sendBroadcast(Intent(ACTION_AWARE_BATTERY_CHANGED))
            }
        }

        dbEngine?.getLatest(BatteryData.TABLE_NAME, callback = callback) ?: callback(null)
    }

    private fun onPowerConnected() {
        // val powerConnected: (AwareData?, AwareData?) -> Unit = { lastBattery, lastDischarge ->
        //     lastBattery?.withData<BatteryData> { batteryData ->
        //         if (lastDischarge != null) {
        //             lastDischarge.alterData<BatteryDischarge> {
        //                 if (it.endTimestamp == 0L) {
        //                     it.end = batteryData.level
        //                     it.endTimestamp = System.currentTimeMillis()
        //                 }
        //             }
        //             dbEngine?.update(lastDischarge)
        //         }

        val batteryCharge = BatteryCharge().apply {
            timestamp = System.currentTimeMillis()
            deviceId = CONFIG.deviceId
            // start = batteryData.level
        }

        dbEngine?.save(batteryCharge, BatteryCharge.TABLE_NAME)
        //     }
        // }

        // val batteryDataCallback: (AwareData?) -> Unit = { batteryData ->
        //     dbEngine?.getLatest(BatteryDischarge.TABLE_NAME, callback = { batteryDischarge ->
        //         powerConnected(batteryData, batteryDischarge)
        //     }) ?: powerConnected(batteryData, null)
        // }

        // dbEngine?.getLatest(BatteryData.TABLE_NAME, callback = batteryDataCallback)
        //         ?: batteryDataCallback(null)

        logd(ACTION_AWARE_BATTERY_CHARGING)
        applicationContext.sendBroadcast(Intent(ACTION_AWARE_BATTERY_CHARGING))

        CONFIG.sensorObserver?.onBatteryCharging()
    }

    private fun onPowerDisconnected() {
        // val powerDisconnected: (AwareData?, AwareData?) -> Unit = { lastBattery, lastCharge ->
        //     lastBattery?.withData<BatteryData> { batteryData ->
        //         if (lastCharge != null) {
        //             lastCharge.alterData<BatteryDischarge> {
        //                 if (it.endTimestamp == 0L) {
        //                     it.end = batteryData.level
        //                     it.endTimestamp = System.currentTimeMillis()
        //                 }
        //             }
        //             dbEngine?.update(lastCharge)
        //         }

        val batteryDischarge = BatteryDischarge().apply {
            timestamp = System.currentTimeMillis()
            deviceId = CONFIG.deviceId
            // start = batteryData.level
        }

        dbEngine?.save(batteryDischarge, BatteryDischarge.TABLE_NAME)
        //     }

        // }

        // val batteryDataCallback: (AwareData?) -> Unit = { batteryData ->
        //     dbEngine?.getLatest(BatteryCharge.TABLE_NAME, callback = { batteryCharge ->
        //         powerDisconnected(batteryData, batteryCharge)
        //     }) ?: powerDisconnected(batteryData, null)
        // }

        // dbEngine?.getLatest(BatteryData.TABLE_NAME, callback = batteryDataCallback)
        //         ?: batteryDataCallback(null)


        logd(ACTION_AWARE_BATTERY_DISCHARGING)
        applicationContext.sendBroadcast(Intent(ACTION_AWARE_BATTERY_DISCHARGING))

        CONFIG.sensorObserver?.onBatteryDischarging()
    }

    private fun onBatteryLow() {
        logd(ACTION_AWARE_BATTERY_LOW)

        applicationContext.sendBroadcast(Intent(ACTION_AWARE_BATTERY_LOW))
        CONFIG.sensorObserver?.onBatteryLow()
    }

    private fun onShutDown() {
        val shutDown: (AwareData?) -> Unit = { lastBattery ->
            lastBattery?.withData<BatteryData> { batteryData ->
                val newData = BatteryData().apply {
                    timestamp = System.currentTimeMillis()
                    timezone = TimeZone.getDefault().rawOffset
                    deviceId = CONFIG.deviceId
                    label = CONFIG.label
                    status = STATUS_PHONE_SHUTDOWN
                    level = batteryData.level
                    scale = batteryData.scale
                    voltage = batteryData.voltage
                    temperature = batteryData.temperature
                    adaptor = batteryData.adaptor
                    health = batteryData.health
                    technology = batteryData.technology
                }

                dbEngine?.save(newData, BatteryData.TABLE_NAME)
            }
        }

        dbEngine?.getLatest(BatteryData.TABLE_NAME, callback = shutDown) ?: shutDown(null)

        logd(ACTION_AWARE_PHONE_SHUTDOWN)
        applicationContext.sendBroadcast(Intent(ACTION_AWARE_PHONE_SHUTDOWN))
        CONFIG.sensorObserver?.onPhoneShutdown()
    }

    private fun onReboot() {
        val reboot: (AwareData?) -> Unit = { lastBattery ->
            lastBattery?.withData<BatteryData> { batteryData ->
                val newData = BatteryData().apply {
                    timestamp = System.currentTimeMillis()
                    timezone = TimeZone.getDefault().rawOffset
                    deviceId = CONFIG.deviceId
                    label = CONFIG.label
                    status = STATUS_PHONE_REBOOT
                    level = batteryData.level
                    scale = batteryData.scale
                    voltage = batteryData.voltage
                    temperature = batteryData.temperature
                    adaptor = batteryData.adaptor
                    health = batteryData.health
                    technology = batteryData.technology
                }

                dbEngine?.save(newData, BatteryData.TABLE_NAME)
            }
        }

        dbEngine?.getLatest(BatteryData.TABLE_NAME, callback = reboot)
                ?: reboot(null)

        logd(ACTION_AWARE_PHONE_REBOOT)
        applicationContext.sendBroadcast(Intent(ACTION_AWARE_PHONE_REBOOT))
        CONFIG.sensorObserver?.onPhoneReboot()
    }


    class BatterySensorBroadcastReceiver : AwareSensor.SensorBroadcastReceiver() {

        // companion object {
        //     private val TAG = "BatteryReceiver"
        // }

        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return

            logd("Sensor broadcast received. action: " + intent?.action)

            when (intent?.action) {
                AwareSensor.SensorBroadcastReceiver.SENSOR_START_ENABLED -> {
                    logd("Sensor enabled: " + CONFIG.enabled)

                    if (CONFIG.enabled) {
                        context.startService(Intent(context, BatterySensor::class.java))
                    }
                }

                ACTION_AWARE_BATTERY_STOP,
                AwareSensor.SensorBroadcastReceiver.SENSOR_STOP_ALL -> {
                    logd("Stopping sensor.")
                    context.stopService(Intent(context, BatterySensor::class.java))
                }

                ACTION_AWARE_BATTERY_START -> {
                    context.startService(Intent(context, BatterySensor::class.java))
                }
            }
        }
    }

    private val batterySpecificBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            logd("Sensor broadcast received. action: " + intent.action)

            instance?.run {
                when (intent.action) {
                    ACTION_AWARE_BATTERY_LABEL -> {
                        intent.getStringExtra(EXTRA_AWARE_BATTERY_LABEL)?.let {
                            CONFIG.label = it
                        }
                    }

                    ACTION_AWARE_BATTERY_SYNC -> {
                        onSync(intent)
                    }
                }

                this
            }
        }
    }

    interface SensorObserver {
        fun onBatteryChanged(data: BatteryData)
        fun onPhoneReboot()
        fun onPhoneShutdown()
        fun onBatteryLow()
        fun onBatteryCharging()
        fun onBatteryDischarging()
    }
}

private fun logd(text: String) {
    if (BatterySensor.CONFIG.debug) Log.d(BatterySensor.TAG, text)
}

private fun logw(text: String) {
    Log.w(BatterySensor.TAG, text)
}

private fun loge(text: String) {
    Log.e(BatterySensor.TAG, text)
}