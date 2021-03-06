package com.awareframework.android.sensor.battery.model

import com.awareframework.android.core.model.AwareObject
import com.google.gson.Gson

/**
 * Battery charge data
 *
 * @author  sercant
 * @date 25/04/2018
 */
class BatteryCharge(
//        var start: Int = 0,
//        var end: Int = 0,
//        var endTimestamp: Long = 0L
) : AwareObject(jsonVersion = 2) {

    companion object {
        const val TABLE_NAME = "batteryCharge"

        fun fromJson(json: String) : BatteryCharge {
            return Gson().fromJson(json, BatteryCharge::class.java)
        }
    }

    override fun toString(): String = toJson()
}