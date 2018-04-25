package com.awareframework.android.sensor.battery

import com.awareframework.android.sensor.battery.model.BatteryData

interface BatteryListener {
    fun onBatteryChanged(data: BatteryData)
    fun onPhoneReboot()
    fun onPhoneShutdown()
    fun onBatteryLow()
    fun onBatteryCharging()
    fun onBatteryDischarging()
}