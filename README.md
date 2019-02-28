# Wireless Live Streaming plug-in

# 1. Overview
Wireless Live Streaming plug-in can stream live 360 movie to YouTube or Facebook directly from RICOH THETA V. 

# 2. Terms of Service

> You agree to comply with all applicable export and import laws and regulations applicable to the jurisdiction in which the Software was obtained and in which it is used. Without limiting the foregoing, in connection with use of the Software, you shall not export or re-export the Software  into any U.S. embargoed countries (currently including, but necessarily limited to, Crimea – Region of Ukraine, Cuba, Iran, North Korea, Sudan and Syria) or  to anyone on the U.S. Treasury Department’s list of Specially Designated Nationals or the U.S. Department of Commerce Denied Person’s List or Entity List.  By using the Software, you represent and warrant that you are not located in any such country or on any such list.  You also agree that you will not use the Software for any purposes prohibited by any applicable laws, including, without limitation, the development, design, manufacture or production of missiles, nuclear, chemical or biological weapons.

By using the Wireless Live Streaming plug-in, you are agreeing to the above and the license terms, [LICENSE.txt](LICENSE.txt).

Copyright &copy; 2018 Ricoh Company, Ltd.

# 3. Build and Use Environment

## 3-1. Hardware

* RICOH THETA V
* Firmware ver.2.31.1 and above

    > Information on checking and updating the firmware is [here](https://theta360.com/en/support/manual/v/content/pc/pc_09.html).

## 3-2.  Development Environment

This plug-in has been built under the following conditions.

#### Operating System

* Windows&trade; 10 Version 1709
* macOS&reg; High Sierra ver.10.13

#### Development environment

* Android&trade; Studio 3.1+
* gradle 3.1.3
* Android SDK (API Level 25)
* compileSdkVersion 26
* buildToolsVersion "27.0.3"
* minSdkVersion 25
* targetSdkVersion 25

# 4. Install
Android Studio install apk after build automatically. Or use the following command after build.

```
adb install -r app-debug.apk
```

### Give permissions for this plug-in.

  Using desktop viewing app as Vysor, open Settings app and turns on the permissions at "Apps" > "Wireless Live Streaming" > "Permissions"

# 5. How to Use
If "Stream Name" can be used repeatedly, the plugin setting (entering "Stream Name" and "Primary Server URL") is needed only once at the first time. In this case, after setting-up streaming event on YouTube / Facebook, user can start streaming just pressing shutter button.

The following steps is for the first time to use.

If "stream name" can be used repeatedly, plug-in settings (input of "stream name" and "primary server URL") are required only once at the beginning. In this case, you can start streaming just by pressing the shutter button after setting streaming events on YouTube / Facebook.

The following steps are for the first use.
We recommend that the THETA V be close to fully charged before starting the steps.

1. Turn on the THETA.
2. Open RICOH THETA app on your Win/Mac.
3. Set this plug-in as an active plugin from "File" > "Plug-in management..."
4. Connect THETA V to Wireless-LAN by client mode  
   For example, let's assume that there are a THETA V, a macOS machine and an iPhone on the same wireless LAN.  
5. Set active plug-in  
   Open the THETA mobile app on an iOS / Android smartphone
   Tap "Settings" at right bottom corner  
   Confirm "Connection" is "Wi-Fi" or "Wi-Fi+Bluetooth".  
   Tap "Camera settings"  
   Tap "Plug-in"  
   Select "Wireless Live Streaming"  
6. Check IP address of the camera
   Back to the Camera settings  
   Check IP-address of THETA V on smartphone app
   If you use macOS type "dns-sd -q THETAYL01234567.local" in Terminal. Here "THETAYL01234567" is an example, please change to your serial number.
7. Setup "Stream Name" and "Primary Server URL" on [YouTube](http://www.youtube.com/my_live_events) or [Facebook](https://www.facebook.com/live/create).
    Setup [YouTube Ingestion Settings in event](http://www.youtube.com/my_live_events) and check "Stream Name" and "Primary Server URL"
   Select "Reusable stream key" in "Select type of stream key" of the setting. This is the key point to omit the plugin setting from the next.
   If you use [Facebook](https://www.facebook.com/live/create), create live streaming and get persistent stream key.
8. Launch plug-in
   Press Mode button till LED2 turns white or launch plug-in from the smartphone app (RICOH THETA)
9. Open WebUI of plug-in  
    Open the URL (http://(ip-address):8888) on the browser   
    Here, (ip-address) is example. Change it to your THETA V's IP address.  
10. Enter streaming setting
    Put "Primary Server URL" of YouTube to the "Server URL" box in WebUI  
    Put "Stream Name" of YouTube to the "Stream name/key" box in WebUI  
    It is similar to YouTube, even if you use Facebook.
    Select Resolution (e.g. 4K(3840x1920) 30fps)
    Select Bit rates from the list. If you select "Auto", the camera will need to stream once to check the bandwidth of the network.  
    Press "Fix streaming settings" button
11. Prepare streaming on [YouTube](http://www.youtube.com/my_live_events) or [Facebook live](https://www.facebook.com/live/create)
    Please use the same stream name specified at step.7.
12. Start streaming from THETA V
    Press Shutter key  
     or   
    Press "Start streaming" button in the WebUI  
13. Check preview on YouTube's "Live Control Room" or Facebook live.
14. Start streaming from YouTube or Facebook
15. On the air
16. Stop streaming from YouTube or Facebook
17. Stop streaming from THETA V
    Press Shutter key  
     or   
    Press "Stop streaming" button in the WebUI  

# 6. History
* ver.1.0.9 (2018/07/23): Initial version.

---

## Trademark Information

The names of products and services described in this document are trademarks or registered trademarks of each company.

* Android, Nexus, Google Chrome, Google Play, Google Play logo, Google Maps, Google+, Gmail, Google Drive, Google Cloud Print and YouTube are trademarks of Google Inc.
* Apple, Apple logo, Macintosh, Mac, Mac OS, OS X, AppleTalk, Apple TV, App Store, AirPrint, Bonjour, iPhone, iPad, iPad mini, iPad Air, iPod, iPod mini, iPod classic, iPod touch, iWork, Safari, the App Store logo, the AirPrint logo, Retina and iPad Pro are trademarks of Apple Inc., registered in the United States and other countries. The App Store is a service mark of Apple Inc.
* Microsoft, Windows, Windows Vista, Windows Live, Windows Media, Windows Server System, Windows Server, Excel, PowerPoint, Photosynth, SQL Server, Internet Explorer, Azure, Active Directory, OneDrive, Outlook, Wingdings, Hyper-V, Visual Basic, Visual C ++, Surface, SharePoint Server, Microsoft Edge, Active Directory, BitLocker, .NET Framework and Skype are registered trademarks or trademarks of Microsoft Corporation in the United States and other countries. The name of Skype, the trademarks and logos associated with it, and the "S" logo are trademarks of Skype or its affiliates.
* Wi-Fi™, Wi-Fi Certified Miracast, Wi-Fi Certified logo, Wi-Fi Direct, Wi-Fi Protected Setup, WPA, WPA 2 and Miracast are trademarks of the Wi-Fi Alliance.
* The official name of Windows is Microsoft Windows Operating System.
* All other trademarks belong to their respective owners.
