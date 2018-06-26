package com.awareframework.android.sensor.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.awareframework.android.core.db.Engine
import com.awareframework.android.core.manager.DbSyncManager
import com.awareframework.android.sensor.battery.Battery
import com.awareframework.android.sensor.battery.BatteryObserver
import com.awareframework.android.sensor.battery.model.BatteryData
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val CHANNEL_ID = "com.awareframework.android.sensor.example"
        const val GROUP_ID = "sensor logs"
        const val DEVICE_ID = "device_id"
    }

    private lateinit var battery: Battery
    private lateinit var syncManager: DbSyncManager

    private var notificationId: Int = 0

    private val sensorObserver: BatteryObserver = object : BatteryObserver {
        override fun onBatteryChanged(data: BatteryData) {
            logd(data.toJson())
        }

        override fun onPhoneReboot() {
            logd("Phone reboot.")
        }

        override fun onPhoneShutdown() {
            logd("Phone shutdown")
        }

        override fun onBatteryLow() {
            logd("Phone battery low")
        }

        override fun onBatteryCharging() {
            logd("Phone charging")
        }

        override fun onBatteryDischarging() {
            logd("Phone discharging")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        battery = Battery.Builder(this).run {
            setDebug(true)
            setSensorObserver(sensorObserver)
            setDatabaseType(Engine.DatabaseType.ROOM)
            setDatabaseHost("http://10.0.2.2:3000/insert")
            setDeviceId(getDeviceId())
        }.build()

        syncManager = DbSyncManager.Builder(this).run {
            setBatteryChargingOnly(false)
            setDebug(true)
            setWifiOnly(false)
            setSyncInterval(0.5f)
        }.build()

        battery.start()

        syncManager.start()

    }

    fun logd(text: String) {
        Log.d("BatteryExample", text)

        createNotificationChannel()

        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Battery Event")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setGroup(GROUP_ID)
                .setAutoCancel(true)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.notify(notificationId++, mBuilder.build())
    }

    private fun createNotificationChannel(): String {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Example Battery Sensor"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)

            CHANNEL_ID
        } else {
            ""
        }
    }

    private fun getDeviceId(): String {
        return getPreferences(Context.MODE_PRIVATE).getString(DEVICE_ID, makeDeviceId())
    }

    private fun makeDeviceId(): String {
        val deviceId = UUID.randomUUID().toString()
        getPreferences(Context.MODE_PRIVATE).edit().putString(DEVICE_ID, deviceId).apply()

        return deviceId
    }
}
