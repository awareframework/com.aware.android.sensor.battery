package com.awareframework.android.sensor.battery

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.awareframework.android.core.db.Engine
import com.awareframework.android.sensor.battery.model.BatteryData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 * <p>
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.aware.android.sensor.battery.test", appContext.packageName)

        BatterySensor.start(appContext, BatterySensor.Config().apply {
            sensorObserver = object : BatterySensor.Observer {
                override fun onBatteryChanged(data: BatteryData) {
                    // your code here...
                }

                override fun onPhoneReboot() {
                    // your code here...
                }

                override fun onPhoneShutdown() {
                    // your code here...
                }

                override fun onBatteryLow() {
                    // your code here...
                }

                override fun onBatteryCharging() {
                    // your code here...
                }

                override fun onBatteryDischarging() {
                    // your code here...
                }

            }
            dbType = Engine.DatabaseType.ROOM
            debug = true
            // more configuration...
        })
    }
}
