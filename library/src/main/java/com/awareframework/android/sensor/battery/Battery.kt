package com.awareframework.android.sensor.battery

import android.content.Context
import android.content.Intent
import com.awareframework.android.core.model.ISensorController
import com.awareframework.android.core.model.SensorConfig


/**
 * Battery controller class
 *
 * @author  sercant
 * @date 23/04/2018
 */
class Battery private constructor(private val context: Context) : ISensorController {

    // TODO (sercant): change to data class when it has fields
    class BatteryConfig(
            var batteryListener: BatteryListener? = null
    ) : SensorConfig(dbPath = "aware_battery")

    class Builder(private val context: Context) {
        val config: BatteryConfig = BatterySensor.CONFIG

        fun build(): Battery = Battery(context)
    }

    override fun disable() {
        BatterySensor.CONFIG.enabled = false
    }

    override fun enable() {
        BatterySensor.CONFIG.enabled = true
    }

    override fun isEnabled(): Boolean = BatterySensor.CONFIG.enabled

    override fun start() {
        context.startService(Intent(context, BatterySensor::class.java))
    }

    override fun stop() {
        context.stopService(Intent(context, BatterySensor::class.java))
    }

    override fun sync(force: Boolean) {
        BatterySensor.instance?.onSync(null)
    }

}