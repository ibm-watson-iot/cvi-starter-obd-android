<!------------------------------------------------------------------------------------------------
   Copyright 2016,2019 IBM Corp. All Rights Reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-------------------------------------------------------------------------------------------------->
# IBM IoT Connected Vehicle Insights - OBDII Fleet Management App for Android


## Overview
The IBM IoT Connected Vehicle Insights - Mobility Starter Application uses the **IBM IoT Platform** that is available on **IBM Cloud** to help you to quickly build a smart fleet management solution. The solution consists of a mobile app and a server component which is the **IBM IoT Connected Vehicle Insights - Fleet Management Starter Application**.

### Mobile app
The starter app provides a mobile app to connect to an OBDII dongle plugged in to your car. If you are a user of the application, you can use the mobile app to do the following tasks:

- See real-time data from your car on the screen
- Beam the data to the IBM IoT Platform, which will automatically get synced to the **Fleet Management Web Application**

While you drive the car, the service tracks your location and also records the health of your car. This will happen in the background, which means you could lock your phone in the meantime or use other applications.

Once you want to stop the application from recording your data, simply press "End Session", and the application will close.

You can currently download and install the mobile app on your Android mobile device.

### Server component
The "IBM IoT Connected Vehicle Insights - OBDII Fleet Management App" interacts with a server component. The server component provides the back-end fleet management and system monitoring service that provides more features for fleet management companies. By default, the mobile app connects to a test server that is provided by IBM. You can also choose to deploy your own server instance to IBM Cloud and connect your mobile app to that instance instead of the test system. For more information about deploying the fleet management server component, see [ibm-watson-iot/cvi-starter-server-fm](https://github.com/ibm-watson-iot/cvi-starter-server-fm).


## Prerequisites

Before you deploy the Android application, ensure that the following prerequisites are met:

- Deploy the Fleet Management Starter Application, see [ibm-watson-iot/cvi-starter-server-fm](https://github.com/ibm-watson-iot/cvi-starter-server-fm).
- Install the Android Studio integrated development environment (IDE) V3.3.1 or later.
- Install an Android emulator device that is running on at least API Level 21.
- The sample source code for the mobile app is supported only for use with an Android device and is intended to be used in conjunction with officially licensed Android development tools and further customized tools that are distributed under the terms and conditions of your licensed Android Developer Program.


## Deploying the mobile app

To try the Android application by using Android Emulator, complete the following steps.

1. Clone the source code repository of the OBDII application for Android.
   1. Create a project folder on a local PC.
   1. Clone the following source code repository to the new project folder, Git clone https://github.com/ibm-watson-iot/cvi-starter-obd-android.
   1. Install Android Studio on the local PC, see https://developer.android.com/studio/install.  If you see the following type of  'Gradle sync' error message, click OK to use the Gradle wrapper.  To read about this Gradle issue and other issues see https://github.com/flutter/flutter/issues/26832.
    ![Gradle Sync Dialog](GradleSync.jpg)
   1. Open the project folder in Android Studio.

2. With Android Studio, open a root folder of the cloned repository.
    1. In Android Studio, select 'Open an existing Android Studio project'.
    1. Select the directory where you cloned the source code.
    1. If a message about 'Android Gradle Plugin Update Recommended' displays, select 'Update'.
    1. Select the directory where you cloned the source code.

3. Run the OBDII app with Android Emulator in Android Studio.
    1. From the Android Studio toolbar, select 'Run app'
    1. Select deployment target.
    1. Create a virtual device if a device does not exist.
    1. On the Disclaimer, click AGREE.
    1. For location access, click ALLOW.
    1. Click the QR code image.
    1. Click the MANUAL SETUP button.
    1. In the left pane, select 'Settings'.
    1. Enter details for the following items. Fleet Management application URL, Username and Password to Fleet Management Application settings. For example, https://iota-starter-server-fleetmgmt.mybluemix.net/webclient/map.
    1. Click left arrow to go back to the main page.
    1. Click OK to register your device.
    1. On 'Your Device is Now Registered!', select CLOSE.
    1. In the Android Emulator toolbar, select the '...' icon.
    1. In the Location page, enter the longitude and latitude details.
    1. Click the SEND button.
    1. Check that your device appears in the Fleet Management App.

4. Run the OBDII app with your mobile phone in Android Studio
    1. In a browser, open your Fleet Management Starter Application, the server component, that is deployed on IBM Cloud. For example, https://iota-starter-server-fleetmgmt.mybluemix.net/webclient/map.
    1. In the left pane, select 'Settings'. A QR code displays, the QR code is used in a latter step.
    1. In Android Studio, within the toolbar, click the run icon.
    1. Select deployment target. For your mobile phone, see [Android Studio Developers](https://developer.android.com/studio/run/device).
    1. On the Disclaimer, click AGREE.
    1. For location access, click ALLOW.
    1. Tap the QR code image.
    1. Scan the QR code.
    1. Tap OK to register your device.
    1, On 'Your Device is Now Registered!', select CLOSE.
    1. Check that your device appears in the Fleet Management App.


## Reporting defects
To report a defect with the IBM IoT Connected Vehicle Insights - Mobility Starter Application mobile app, go to the [Issues](https://github.com/ibm-watson-iot/cvi-starter-obd-android/issues) section.

## Privacy notice
The "IBM IoT Connected Vehicle Insights - OBDII Fleet Management App for Android" on IBM Cloud stores all of the driving data that is obtained while you use the mobile app.

## Questions, comments or suggestions
For your questions, comments or suggestions to us, visit [IBM Community for IBM IoT Connected Vehicle Insights](https://community.ibm.com/community/user/imwuc/communities/globalgrouphome?CommunityKey=eaea64a5-fb9b-4d78-b1bd-d87dc70e8171).

## Useful links

- [IBM Cloud](https://cloud.ibm.com)
- [IBM Cloud Documentation](https://cloud.ibm.com/docs)
- [IBM Cloud Developers Community](https://developer.ibm.com/depmodels/cloud)
- [IBM Watson Internet of Things](http://www.ibm.com/internet-of-things)
- [IBM Watson IoT Platform](https://www.ibm.com/internet-of-things/solutions/iot-platform/watson-iot-platform)
- [IBM Watson IoT Platform Developers Community](https://developer.ibm.com/iotplatform)
- [IBM Marketplace: IBM IoT Connected Vehicle Insights](https://www.ibm.com/us-en/marketplace/iot-for-automotive)
