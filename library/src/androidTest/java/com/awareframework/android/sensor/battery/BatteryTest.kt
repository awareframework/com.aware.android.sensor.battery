package com.awareframework.android.sensor.battery

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class BatteryTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.awareframework.android.sensor.battery.test", appContext.packageName)

        val battery = Battery.Builder(appContext).apply {
            config.debug = true
        }.build()

        battery.start()

        sleep(100)

        battery.start()

        sleep(100)

        battery.start()

        sleep(20000)
    }
}
