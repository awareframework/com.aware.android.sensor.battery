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

        /**
         * Broadcasted event: the battery values just changed
         */
        const val ACTION_AWARE_BATTERY_CHANGED = "ACTION_AWARE_BATTERY_CHANGED"

        /**
         * Broadcasted event: the user just started charging
         */
        const val ACTION_AWARE_BATTERY_CHARGING = "ACTION_AWARE_BATTERY_CHARGING"

        /**
         * Broadcasted event: battery charging over power supply (AC)
         */
        const val ACTION_AWARE_BATTERY_CHARGING_AC = "ACTION_AWARE_BATTERY_CHARGING_AC"

        /**
         * Broadcasted event: battery charging over USB
         */
        const val ACTION_AWARE_BATTERY_CHARGING_USB = "ACTION_AWARE_BATTERY_CHARGING_USB"

        /**
         * Broadcasted event: the user just stopped charging and is running on battery
         */
        const val ACTION_AWARE_BATTERY_DISCHARGING = "ACTION_AWARE_BATTERY_DISCHARGING"

        /**
         * Broadcasted event: the battery is fully charged
         */
        const val ACTION_AWARE_BATTERY_FULL = "ACTION_AWARE_BATTERY_FULL"

        /**
         * Broadcasted event: the battery is running low and should be charged ASAP
         */
        const val ACTION_AWARE_BATTERY_LOW = "ACTION_AWARE_BATTERY_LOW"

        /**
         * Broadcasted event: the phone is about to be shutdown.
         */
        const val ACTION_AWARE_PHONE_SHUTDOWN = "ACTION_AWARE_PHONE_SHUTDOWN"

        /**
         * Broadcasted event: the phone is about to be rebooted.
         */
        const val ACTION_AWARE_PHONE_REBOOT = "ACTION_AWARE_PHONE_REBOOT"

        /**
         * [Battery_Data.STATUS] Phone shutdown
         */
        const val STATUS_PHONE_SHUTDOWN = -1

        /**
         * [Battery_Data.STATUS] Phone rebooted
         */
        const val STATUS_PHONE_REBOOT = -2

        /**
         * [Battery_Data.STATUS] Phone finished booting
         */
        const val STATUS_PHONE_BOOTED = -3 // TODO

        val CONFIG = Battery.BatteryConfig()

        var instance: BatterySensor? = null
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

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_SHUTDOWN)
            addAction(Intent.ACTION_REBOOT)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        registerReceiver(batteryBroadcastReceiver, filter)

        logd("Battery service created!")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // TODO (sercant): check permissions

        logd("Battery service active...")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(batteryBroadcastReceiver)

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
        // TODO (sercant): find out a way to remove the synced data without affecting the functionality
        dbEngine?.startSync(BatteryData.TABLE_NAME, DbSyncConfig(removeAfterSync = false))
        dbEngine?.startSync(BatteryCharge.TABLE_NAME, DbSyncConfig(removeAfterSync = false))
        dbEngine?.startSync(BatteryDischarge.TABLE_NAME, DbSyncConfig(removeAfterSync = false))
    }

    private fun onBatteryChanged(extras: Bundle) {
        val lastBattery = dbEngine?.getLatest(BatteryData.TABLE_NAME)

        var changed = true
        lastBattery?.withData<BatteryData> { data ->
            changed = extras.getInt(BatteryManager.EXTRA_LEVEL) != data.level ||
                    extras.getInt(BatteryManager.EXTRA_PLUGGED) != data.adaptor ||
                    extras.getInt(BatteryManager.EXTRA_STATUS) != data.status
        }

        if (!changed) return

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
        CONFIG.batteryListener?.onBatteryChanged(newData)


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

    private fun onPowerConnected() {
        val lastBattery = dbEngine?.getLatest(BatteryData.TABLE_NAME)
        val lastDischarge = dbEngine?.getLatest(BatteryDischarge.TABLE_NAME)

        lastBattery?.withData<BatteryData> { batteryData ->
            if (lastDischarge != null) {
                lastDischarge.alterData<BatteryDischarge> {
                    if (it.endTimestamp == 0L) {
                        it.end = batteryData.level
                        it.endTimestamp = System.currentTimeMillis()
                    }
                }
                dbEngine?.update(lastDischarge)
            }

            val batteryCharge = BatteryCharge().apply {
                timestamp = System.currentTimeMillis()
                deviceId = CONFIG.deviceId
                start = batteryData.level
            }

            dbEngine?.save(batteryCharge, BatteryCharge.TABLE_NAME)
        }

        logd(ACTION_AWARE_BATTERY_CHARGING)
        applicationContext.sendBroadcast(Intent(ACTION_AWARE_BATTERY_CHARGING))

        CONFIG.batteryListener?.onBatteryCharging()
    }

    private fun onPowerDisconnected() {
        val lastBattery = dbEngine?.getLatest(BatteryData.TABLE_NAME)
        val lastCharge = dbEngine?.getLatest(BatteryCharge.TABLE_NAME)

        lastBattery?.withData<BatteryData> { batteryData ->
            if (lastCharge != null) {
                lastCharge.alterData<BatteryDischarge> {
                    if (it.endTimestamp == 0L) {
                        it.end = batteryData.level
                        it.endTimestamp = System.currentTimeMillis()
                    }
                }
                dbEngine?.update(lastCharge)
            }

            val batteryDischarge = BatteryDischarge().apply {
                timestamp = System.currentTimeMillis()
                deviceId = CONFIG.deviceId
                start = batteryData.level
            }

            dbEngine?.save(batteryDischarge, BatteryDischarge.TABLE_NAME)
        }

        logd(ACTION_AWARE_BATTERY_DISCHARGING)
        applicationContext.sendBroadcast(Intent(ACTION_AWARE_BATTERY_DISCHARGING))

        CONFIG.batteryListener?.onBatteryDischarging()
    }

    private fun onBatteryLow() {
        logd(ACTION_AWARE_BATTERY_LOW)

        applicationContext.sendBroadcast(Intent(ACTION_AWARE_BATTERY_LOW))
        CONFIG.batteryListener?.onBatteryLow()
    }

    private fun onShutDown() {
        val lastBattery = dbEngine?.getLatest(BatteryData.TABLE_NAME)
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

        logd(ACTION_AWARE_PHONE_SHUTDOWN)
        applicationContext.sendBroadcast(Intent(ACTION_AWARE_PHONE_SHUTDOWN))
        CONFIG.batteryListener?.onPhoneShutdown()
    }

    private fun onReboot() {
        val lastBattery = dbEngine?.getLatest(BatteryData.TABLE_NAME)
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

        logd(ACTION_AWARE_PHONE_REBOOT)
        applicationContext.sendBroadcast(Intent(ACTION_AWARE_PHONE_REBOOT))
        CONFIG.batteryListener?.onPhoneReboot()
    }

    private fun logd(text: String) {
        if (CONFIG.debug) Log.d(TAG, text)
    }

    private fun logw(text: String) {
        Log.w(TAG, text)
    }

    private fun loge(text: String) {
        Log.e(TAG, text)
    }
}