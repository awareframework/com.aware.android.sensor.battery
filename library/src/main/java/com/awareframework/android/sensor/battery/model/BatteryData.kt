package com.awareframework.android.sensor.battery.model

import com.awareframework.android.core.model.AwareObject
import com.google.gson.Gson

/**
 * Class decription
 *
 * @author  sercant
 * @date 25/04/2018
 */
class BatteryData(
        var status: Int = 0,
        var level: Int = 0,
        var scale: Int = 0,
        var voltage: Int = 0,
        var temperature: Int = 0,
        var adaptor: Int = 0,
        var health: Int = 0,
        var technology: String = ""
) : AwareObject(jsonVersion = 1) {

    companion object {
        const val TABLE_NAME = "batteryData"

        fun fromJson(json: String) : BatteryData {
            return Gson().fromJson(json, BatteryData::class.java)
        }
    }
}