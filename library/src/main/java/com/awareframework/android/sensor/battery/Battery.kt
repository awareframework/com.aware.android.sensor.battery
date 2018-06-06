package com.awareframework.android.sensor.battery

import android.content.Context
import android.content.Intent
import com.awareframework.android.core.db.Engine
import com.awareframework.android.core.model.ISensorController
import com.awareframework.android.core.model.SensorConfig


/**
 * Battery controller class
 *
 * @author  sercant
 * @date 23/04/2018
 */
class Battery private constructor(private val context: Context) : ISensorController {

    companion object {
        /**
         * Starts the sensor.
         */
        const val ACTION_AWARE_BATTERY_START = "com.aware.android.sensor.battery.SENSOR_START"

        /**
         * Stops the sensor.
         */
        const val ACTION_AWARE_BATTERY_STOP = "com.aware.android.sensor.battery.SENSOR_STOP"

        /**
         * Changes the label of the data.
         */
        const val ACTION_AWARE_BATTERY_LABEL = "com.aware.android.sensor.battery.SET_LABEL"

        const val ACTION_AWARE_BATTERY_SYNC = "com.aware.android.sensor.battery.SYNC"
        const val ACTION_AWARE_BATTERY_SYNC_SENT = "com.aware.android.sensor.battery.SYNC_SENT"

        const val EXTRA_AWARE_BATTERY_LABEL = "label"

        // Sensor actions

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
    }

    data class BatteryConfig(
            var batteryObserver: BatteryObserver? = null
    ) : SensorConfig(dbPath = "aware_battery")

    class Builder(private val context: Context) {
        val config: BatteryConfig = BatterySensor.CONFIG

        /**
         * @param label collected data will be labeled accordingly. (default = "")
         */
        fun setLabel(label: String) = apply { config.label = label }

        /**
         * @param debug enable/disable logging to Logcat. (default = false)
         */
        fun setDebug(debug: Boolean) = apply { config.debug = debug }

        /**
         * @param key encryption key for the database. (default = no encryption)
         */
        fun setDatabaseEncryptionKey(key: String) = apply { config.dbEncryptionKey = key }

        /**
         * @param host host for syncing the database. (default = null)
         */
        fun setDatabaseHost(host: String) = apply { config.dbHost = host }

        /**
         * @param type which db engine to use for saving data. (default = NONE)
         */
        fun setDatabaseType(type: Engine.DatabaseType) = apply { config.dbType = type }

        /**
         * @param path path of the database.
         */
        fun setDatabasePath(path: String) = apply { config.dbPath = path }

        /**
         * @param observer will get the instant events that are collected by the sensor.
         */
        fun setSensorObserver(observer: BatteryObserver) = apply { config.batteryObserver = observer }

        /**
         * @param deviceId id of the device that will be associated with the events and the sensor. (default = "")
         */
        fun setDeviceId(deviceId: String) = apply { config.deviceId = deviceId }

        fun build(): Battery = Battery(context)
    }

    /**
     * Marks the sensor as disabled. Doesn't stop the service if it's running.
     */
    override fun disable() {
        BatterySensor.CONFIG.enabled = false
    }

    /**
     * Marks the sensor as enabled. Doesn't run the service if it's not running.
     */
    override fun enable() {
        BatterySensor.CONFIG.enabled = true
    }

    override fun isEnabled(): Boolean = BatterySensor.CONFIG.enabled

    /**
     * Starts the Aware Battery Service
     */
    override fun start() {
        context.startService(Intent(context, BatterySensor::class.java))
    }

    /**
     * Stops the Aware Battery Service
     */
    override fun stop() {
        context.stopService(Intent(context, BatterySensor::class.java))
    }

    /**
     * Sends sync command to the service
     */
    override fun sync(force: Boolean) {
        // TODO (sercant): implement non-forced sync
        BatterySensor.instance?.onSync(null)
    }

}