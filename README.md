# AWARE Battery

[![jitpack-badge](https://jitpack.io/v/awareframework/com.aware.android.sensor.battery.svg)](https://jitpack.io/#awareframework/com.aware.android.sensor.battery)

The Battery sensor monitors battery information and monitors power related events (e.g., phone shutting down, rebooting). This sensor provides user-driven contexts, such as initiating a charge and unplugging the device.

## Public functions

### Battery

This is the main interaction controller with the sensor for the programmers.

+ `start()`: Starts the battery sensor with the prebuilt configuration.
+ `stop()`: Stops the battery service.
+ `sync(force: Boolean)`: sends sync signal to the sensor. `force` determines if the signal should go through the configured `SyncManager` or directly to the database `Engine`.
+ `isEnabled(): Boolean`: returns the state information about if the sensor is configured to be enabled.
+ `enable()`: enables the sensor in the configuration.
+ `disable()`: disables the sensor in the configuration.

### Battery.Builder

A builder class for building an instance of an battery controller.

+ `setLabel(label: String)`: collected data will be labeled accordingly. (default = "")
+ `setDebug(debug: Boolean)`: enable/disable logging to `Logcat`. (default = false)
+ `setDatabaseHost(host: String)`: host for syncing the database. (default = null)
+ `setDatabaseEncryptionKey(key: String)`: Encryption key for the database. (default = no encryption)
+ `setDatabaseHost(host: String)`: Host for syncing the database. (default = null)
+ `setDatabaseType(type: Engine.DatabaseType)`: Which db engine to use for saving data. (default = NONE)
+ `setDatabasePath(path: String)`: Path of the database.
+ `setSensorObserver(sensorObserver: BatteryObserver)`: Callback for live data updates.
+ `setDeviceId(deviceId: String)`: Id of the device that will be associated with the events and the sensor. (default = "")

## Broadcasts

+ `Battery.ACTION_AWARE_BATTERY_CHANGED` broadcasted when the battery information changes.
+ `Battery.ACTION_AWARE_BATTERY_CHARGING` broadcasted when the device starts to charge.
+ `Battery.ACTION_AWARE_BATTERY_CHARGING_AC` broadcasted when the device is charging using the power supply (AC).
+ `Battery.ACTION_AWARE_BATTERY_CHARGING_USB` broadcasted when the device is charging using USB.
+ `Battery.ACTION_AWARE_BATTERY_DISCHARGING` broadcasted when the device is unplugged and is running on battery.
+ `Battery.ACTION_AWARE_BATTERY_FULL` broadcasted when the device has finished charging.
+ `Battery.ACTION_AWARE_BATTERY_LOW` broadcasted when the device is low on battery (15% or less).
+ `Battery.ACTION_AWARE_PHONE_SHUTDOWN` broadcasted when the device is about to shutdown.
+ `Battery.ACTION_AWARE_PHONE_REBOOT` broadcasted when the device is about to reboot.

## Data Representations

### Battery Data

| Field       | Type   | Description                                                                     |
| ----------- | ------ | ------------------------------------------------------------------------------- |
| status      | Int    | One of the [Android’s battery status][1], phone shutdown (-1) or rebooted (-2) |
| level       | Int    | Battery level, between 0 and SCALE                                              |
| scale       | Int    | Maximum battery level                                                           |
| voltage     | Int    | Current battery voltage                                                         |
| temperature | Int    | Current battery temperature                                                     |
| adaptor     | Int    | One of the [Android’s battery plugged][2] values                               |
| health      | Int    | One of the [Android’s battery health][3] values                                |
| technology  | String | Battery chemical technology (e.g., Li-Ion, etc.)                                |
| deviceId    | String | AWARE device UUID                                                               |
| timestamp   | Long   | unixtime milliseconds since 1970                                                |
| timezone    | Int    | Timezone of the device                                                          |
| os          | String | Operating system of the device (ex. android)                                    |

[1]: https://developer.android.com/reference/android/os/BatteryManager#BATTERY_STATUS_CHARGING
[2]: http://developer.android.com/reference/android/os/BatteryManager.html#BATTERY_PLUGGED_AC
[3]: http://developer.android.com/reference/android/os/BatteryManager.html#BATTERY_HEALTH_COLD

### Battery Discharges

| Field        | Type   | Description                                       |
| ------------ | ------ | ------------------------------------------------- |
| start        | Int    | Battery level when the device started discharging |
| end          | Int    | Battery level when the device stopped discharging |
| endTimestamp | Long   | time instance of the end of discharge             |
| deviceId     | String | AWARE device UUID                                 |
| timestamp    | Long   | unixtime milliseconds since 1970                  |
| timezone     | Int    | Timezone of the device                            |
| os           | String | Operating system of the device (ex. android)      |

### Battery Charge

| Field        | Type   | Description                                    |
| ------------ | ------ | ---------------------------------------------- |
| start        | Int    | Battery level when the device started charging |
| end          | Int    | Battery level when the device stopped charging |
| endTimestamp | Long   | time instance of the end of charge             |
| deviceId     | String | AWARE device UUID                              |
| timestamp    | Long   | unixtime milliseconds since 1970               |
| timezone     | Int    | Timezone of the device                         |
| os           | String | Operating system of the device (ex. android)   |

## Example usage

```kotlin
val battery = Battery.Builder(appContext)
        .setDebug(true)
        .setDatabaseType(Engine.DatabaseType.ROOM)
        .setSensorObserver(object : BatteryObserver {
            override fun onBatteryChanged(data: BatteryData) {
                // your code here.
            }

            override fun onPhoneReboot() {
                // your code here.
            }

            override fun onPhoneShutdown() {
                // your code here.
            }

            override fun onBatteryLow() {
                // your code here.
            }

            override fun onBatteryCharging() {
                // your code here.
            }

            override fun onBatteryDischarging() {
                // your code here.
            }

        })
        .build()

battery.start()

// ...

battery.stop()
```

## License

Copyright (c) 2014 AWARE Mobile Context Instrumentation Middleware/Framework (http://www.awareframework.com)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
