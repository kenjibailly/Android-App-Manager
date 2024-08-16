# How does it work?

Add a remote json link in the settings with the following structure:

```json
[
  {
    "name": "Icon Manager",
    "apkUrl": "https://website.com/app.apk",
    "appIconUrl": "https://website.com/app_icon.png",
    "version": "1.0",
    "packageName": "com.website.app"
  }
]
```

Apps will show up on the home page, you can then install the apk through the app if not installed, or update them when a new version is available.
Sends out notifications when apps have update available.

# How to personalize and build your APK

1. Open project in Android Studio
2. Right click art.mindglowing.app_manager folder and select Refractor > Rename, now choose your package identifier
3. Go to Edit > Find > Replace in Files
  1. In the first input box add: art.mindglowing.app_manager
  2. In the second input box add: your package identifier you chose before
  3. Click "Replace All"
4. Replace app/res/drawable/logo.png with your own logo to change the logo inside the navigation menu
5. Open app/res/values/strings.xml
   1. Replace "App Manager" with your own app's desired name
   2. Replace "Mind Glowing - App Manager" with your own desired name for the text in the navigation menu under your logo
6. Replace ic_launcher.png files in the app/res/mipmapp folders to change your app icon
7. Build APK: go to Build > Build App Bundle(s) / APK(s) > Build APK, you can now locate your generated APK file and install / distribute it

# Screenshots

<img src="https://github.com/user-attachments/assets/f44c5928-e2a2-4220-b0c6-9a10668661bc" width="230" height="512">
<img src="https://github.com/user-attachments/assets/c4c9e9c0-da12-4cad-8fd2-71b63ccf6a25" width="230" height="512">
<img src="https://github.com/user-attachments/assets/307fcc33-b034-4a6f-8c53-3e90a0fef0a2" width="230" height="512"> 
