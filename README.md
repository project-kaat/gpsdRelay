# GPSD Relay

<img src="./app/src/main/ic_launcher-playstore.png" width="256">

## Description

This app utilizes the GPS available on an Android device to supply information to a gpsd server.

The app supports blindly relaying whatever NMEA sentences it reveives from the underlying OS location mechanisms, as well as generating it's own [NMEA](https://en.wikipedia.org/wiki/NMEA_0183) sentences using the location data, provided by the OS.

For this to work, the device needs to have a GPS fix available and the gpsd server has to be reachable by the device via IP.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/io.github.project_kaat.gpsdrelay/)

or get it from the [Releases section](https://github.com/project-kaat/gpsdRelay/releases/latest).

### Screenshots

![main tab screenshot](./fastlane/metadata/android/en-US/images/phoneScreenshots/1.png)

![settings tab screenshot](./fastlane/metadata/android/en-US/images/phoneScreenshots/2.png)
