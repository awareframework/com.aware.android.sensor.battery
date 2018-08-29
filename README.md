# AWARE Battery

[![jitpack-badge](https://jitpack.io/v/awareframework/com.aware.android.sensor.battery.svg)](https://jitpack.io/#awareframework/com.aware.android.sensor.battery)

The Battery sensor monitors battery information and monitors power related events (e.g., phone shutting down, rebooting). This sensor provides user-driven contexts, such as initiating a charge and unplugging the device.

## Public functions

### BatterySensor

+ `start(context: Context, config: BatterySensor.Config?)`: Starts the battery sensor with the optional configuration.
+ `stop(context: Context)`: Stops the service.

### BatterySensor.Config

Class to hold the configuration of the sensor.

#### Fields

+ `sensorObserver: BatterySensor.Observer`: Callback for live data updates.
+ `enabled: Boolean` Sensor is enabled or not. (default = false)
+ `debug: Boolean` enable/disable logging to `Logcat`. (default = false)
+ `label: String` Label for the data. (default = "")
+ `deviceId: String` Id of the device that will be associated with the events and the sensor. (default = "")
+ `dbEncryptionKey` Encryption key for the database. (default =String? = null)
+ `dbType: Engine` Which db engine to use for saving data. (default = `Engine.DatabaseType.NONE`)
+ `dbPath: String` Path of the database. (default = "aware_battery")
+ `dbHost: String` Host for syncing the database. (Defult = `null`)

## Broadcasts

### Fired Broadcasts

+ `Battery.ACTION_AWARE_BATTERY_CHANGED` broadcasted when the battery information changes.
+ `Battery.ACTION_AWARE_BATTERY_CHARGING` broadcasted when the device starts to charge.
+ `Battery.ACTION_AWARE_BATTERY_CHARGING_AC` broadcasted when the device is charging using the power supply (AC).
+ `Battery.ACTION_AWARE_BATTERY_CHARGING_USB` broadcasted when the device is charging using USB.
+ `Battery.ACTION_AWARE_BATTERY_DISCHARGING` broadcasted when the device is unplugged and is running on battery.
+ `Battery.ACTION_AWARE_BATTERY_FULL` broadcasted when the device has finished charging.
+ `Battery.ACTION_AWARE_BATTERY_LOW` broadcasted when the device is low on battery (15% or less).
+ `Battery.ACTION_AWARE_PHONE_SHUTDOWN` broadcasted when the device is about to shutdown.
+ `Battery.ACTION_AWARE_PHONE_REBOOT` broadcasted when the device is about to reboot.

### Received Broadcasts

+ `BatterySensor.ACTION_AWARE_BATTERY_START`: received broadcast to start the sensor.
+ `BatterySensor.ACTION_AWARE_BATTERY_STOP`: received broadcast to stop the sensor.
+ `BatterySensor.ACTION_AWARE_BATTERY_SYNC`: received broadcast to send sync attempt to the host.
+ `BatterySensor.ACTION_AWARE_BATTERY_SET_LABEL`: received broadcast to set the data label. Label is expected in the `BatterySensor.EXTRA_LABEL` field of the intent extras.

## Data Representations


### Battery Data

| Field       | Type   | Description                                                                     |
| ----------- | ------ | ------------------------------------------------------------------------------- |
| status      | Int    | One of the [Android’s battery status][2], phone shutdown (-1) or rebooted (-2) |
| level       | Int    | Battery level, between 0 and SCALE                                              |
| scale       | Int    | Maximum battery level                                                           |
| voltage     | Int    | Current battery voltage                                                         |
| temperature | Int    | Current battery temperature                                                     |
| adaptor     | Int    | One of the [Android’s battery plugged][3] values                               |
| health      | Int    | One of the [Android’s battery health][4] values                                |
| technology  | String | Battery chemical technology (e.g., Li-Ion, etc.)                                |
| deviceId    | String | AWARE device UUID                                                               |
| label       | String | Customizable label. Useful for data calibration or traceability                 |
| timestamp   | Long   | unixtime milliseconds since 1970                                                |
| timezone    | Int    | [Raw timezone offset][1] of the device                                          |
| os          | String | Operating system of the device (ex. android)                                    |

### Battery Discharges

| Field        | Type   | Description                                                     |
| ------------ | ------ | --------------------------------------------------------------- |
| start        | Int    | Battery level when the device started discharging               |
| end          | Int    | Battery level when the device stopped discharging               |
| endTimestamp | Long   | time instance of the end of discharge                           |
| deviceId     | String | AWARE device UUID                                               |
| label        | String | Customizable label. Useful for data calibration or traceability |
| timestamp    | Long   | unixtime milliseconds since 1970                                |
| timezone     | Int    | [Raw timezone offset][1] of the device                          |
| os           | String | Operating system of the device (ex. android)                    |

### Battery Charge

| Field        | Type   | Description                                                     |
| ------------ | ------ | --------------------------------------------------------------- |
| start        | Int    | Battery level when the device started charging                  |
| end          | Int    | Battery level when the device stopped charging                  |
| endTimestamp | Long   | time instance of the end of charge                              |
| deviceId     | String | AWARE device UUID                                               |
| label        | String | Customizable label. Useful for data calibration or traceability |
| timestamp    | Long   | unixtime milliseconds since 1970                                |
| timezone     | Int    | [Raw timezone offset][1] of the device                          |
| os           | String | Operating system of the device (ex. android)                    |

## Example usage

```kotlin
// To start the service.
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

// To stop the service
BatterySensor.stop(appContext)
```

## License

Copyright (c) 2018 AWARE Mobile Context Instrumentation Middleware/Framework (http://www.awareframework.com)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

[1]: https://developer.android.com/reference/java/util/TimeZone#getRawOffset()
[2]: http://developer.android.com/reference/android/os/BatteryManager.html#BATTERY_STATUS_CHARGING
[3]: http://developer.android.com/reference/android/os/BatteryManager.html#BATTERY_PLUGGED_AC
[4]: http://developer.android.com/reference/android/os/BatteryManager.html#BATTERY_HEALTH_COLD