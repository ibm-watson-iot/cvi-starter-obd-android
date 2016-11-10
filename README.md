# IBM IoT for Automotive - OBDII Fleet Management App for Android


## Overview
The IBM IoT for Automotive - Mobility Starter Application uses the **IBM IoT Platform** that is available on **IBM Bluemix** to help you to quickly build a smart fleet management solution. The solution consists of a mobile app and a server component which is the **IBM IoT for Automotive - Fleet Management Starter Web Application**.

### Mobile app
The starter app provides a mobile app to connect to an OBDII dongle plugged in to your car. If you are a user of the application, you can use the mobile app to do the following tasks:

- See real-time data from your car on the screen
- Beam the data to the IBM IoT Platform, which will automatically get synced to the **Fleet Management Web Application**

While you drive the car, the service tracks your location and also records the health of your car. This will happen in the background, which means you could lock your phone in the meantime or use other applications.

Once you want to stop the application from recording your data, simply press "End Session", and the application will close.

You can currently download and install the mobile app on your Android mobile device.

### Server component
The IoT for Automotive - Mobility Starter Application mobile app interacts with the server component. The server component provides the back-end car sharing and system monitoring service that provides more features for car-hire companies. By default, the mobile app connects to a test server that is provided by IBM. You can also choose to deploy your own server instance to IBM Bluemix and connect your mobile app to that instance instead of the test system. For more information about deploying the car-sharing server component, see [ibm-watson-iot/iota-starter-server](https://github.com/ibm-watson-iot/iota-starter-server).


## Prerequisites

Before you deploy the IoT for Automotive - Mobility Starter Application mobile app for Android, ensure that the following prerequisites are met:

- Install the Android Studio integrated development environment (IDE) V2.1.1 or later.
- Install an Android emulator device that is running on at least API Level 21.
- The sample source code for the mobile app is supported only for use with an Android device and is intended to be used in conjunction with officially licensed Android development tools and further customized tools that are distributed under the terms and conditions of your licensed Android Developer Program.


## Deploying the mobile app

You can download and install the mobile app on iOS and Android mobile devices. For more information about trying the iOS version of the mobile app, see [IBM IoT for Automotive - Mobility Starter Application mobile app for iOS](https://github.com/ibm-watson-iot/iota-starter-carsharing).

To try the IoT for Automotive - Mobility Starter Application mobile app for Android with Android Emulator, complete the following steps:

1. Clone the Mobility Starter Application source code repository for the sample mobile app by using the following git command:    

    ```$ git clone https://github.com/ibm-watson-iot/iota-starter-carsharing-android```  

2. Open the project in Android Studio.

    If you encounter the following dialog, press 'OK'.    
     
    ![Gradle Sync Dialog](GradleSync.jpg)    
     
    If you encounter the following dialog, do NOT 'Update'.    
 
    ![Gradle Plugin Update Dialog](GradlePluginUpdate.jpg)    

3. Edit the **iota-starter-carsharing-android/app/java/carsharing.starter.automotive.iot.ibm.com.mobilestarterapp/ConnectedDriverAPI/API.java** file, and set the `connectedAppURL` variable to the URL for your IoT for Automotive - Mobility Starter Application server app.
4. Edit the **iota-starter-carsharing-android/app/res/values/google_maps_api.xml** file and replace `YOUR_KEY_HERE` with your `google_maps_key`. For more information, see [Get API Key](https://developers.google.com/maps/documentation/android-api/signup).

5. In Android Studio, run the application by pressing **Run 'app'**.

    If you use a virtual device, make sure that you use one with API level 21 as seen in the following dialog.

    ![Deployment Target](DeploymentTarget.jpg)

6. To deploy the mobile app on your device, see [Build and Run Your App](https://developer.android.com/studio/run/index.html).

## Reporting defects
To report a defect with the IoT for Automotive - Mobility Starter Application mobile app, go to the [Issues](https://github.com/ibm-watson-iot/iota-starter-carsharing-android/issues) section.

## Privacy notice
The IoT for Automotive - Mobility Starter Application on Bluemix stores all of the driving data that is obtained while you use the mobile app.

## Useful links

- [IBM IoT for Automotive](http://www.ibm.com/internet-of-things/iot-industry/iot-automotive)
- [IBM Watson Internet of Things](http://www.ibm.com/internet-of-things/)  
- [IBM Watson IoT Platform](http://www.ibm.com/internet-of-things/iot-solutions/watson-iot-platform/)   
- [IBM Watson IoT Platform Developers Community](https://developer.ibm.com/iotplatform/)
- [IBM Bluemix](https://bluemix.net/)  
- [IBM Bluemix documentation](https://www.ng.bluemix.net/docs/)  
- [IBM Bluemix developers community](http://developer.ibm.com/bluemix) 
