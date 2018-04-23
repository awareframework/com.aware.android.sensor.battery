package com.awareframework.android.sensor.battery

import com.awareframework.android.core.model.ISensorController
import com.awareframework.android.core.model.SensorConfig

/**
 * Battery controller class
 *
 * @author  sercant
 * @date 23/04/2018
 */
class Battery : ISensorController {

    // TODO (sercant): change to data class when it has fields
    class BatteryConfig : SensorConfig(dbPath = "aware_battery")

    override fun disable() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun enable() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEnabled(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun start() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sync(force: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}