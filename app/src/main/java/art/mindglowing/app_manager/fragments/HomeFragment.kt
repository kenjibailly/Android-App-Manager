package art.mindglowing.app_manager.fragments

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import art.mindglowing.app_manager.R
import art.mindglowing.app_manager.utilities.SharedPreferencesUtil
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch


// Data class to represent each app's information
data class AppData(
    val name: String,
    val apkUrl: String,
    val appIconUrl: String,
    val version: String,
    val packageName: String
)

class HomeFragment : Fragment() {

    private lateinit var client: OkHttpClient
    private lateinit var refreshButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        Log.d("View", "Home Fragment View created")

        client = OkHttpClient()

        refreshButton = view.findViewById(R.id.refreshButton)
        refreshButton.setOnClickListener {
            fetchAppData()
        }

        checkStoragePermission()

        fetchAppData()
        return view
    }

    override fun onResume() {
        super.onResume()
        // Call the method to fetch or refresh the app data
        fetchAppData()
    }

    private fun fetchAppData() {
        val links = SharedPreferencesUtil.getLinks(requireContext())

        if (links.isEmpty()) {
            Log.d("HomeFragment", "No links found in shared preferences.")
            return
        } else {
            Log.d("LinksList", links.toString())
        }

        CoroutineScope(Dispatchers.IO).launch {
            val allAppData = mutableListOf<AppData>()
            val latch = CountDownLatch(links.size) // Create a latch to count the number of requests

            for (link in links) {
                val request = Request.Builder()
                    .url(link)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                        latch.countDown() // Decrease the count even if the request fails
                    }

                    override fun onResponse(call: Call, response: Response) {
                        // Check if the response is successful and has the correct content type
                        if (response.isSuccessful && response.header("Content-Type")?.contains("application/json") == true) {
                            response.body?.let { responseBody ->
                                try {
                                    val json = responseBody.string()
                                    Log.d("HomeFragment", "JSON Response from $link: $json")

                                    val gson = Gson()
                                    val appType = object : TypeToken<List<AppData>>() {}.type
                                    val newAppList: List<AppData> = gson.fromJson(json, appType)

                                    allAppData.addAll(newAppList) // Add new apps to the list
                                } catch (e: Exception) {
                                    Log.e("HomeFragment", "Error parsing JSON from $link: ${e.message}")
                                }
                            }
                        } else {
                            Log.e("HomeFragment", "Invalid response from $link: ${response.message}")
                        }
                        latch.countDown() // Decrease the count after processing the response
                    }
                })
            }

            // Wait for all requests to complete
            latch.await() // This will block until count reaches 0

            // Switch to the main thread to update the UI
            withContext(Dispatchers.Main) {
                if (allAppData.isNotEmpty()) {
                    createButtons(allAppData) // Pass the aggregated list to create buttons
                } else {
                    Toast.makeText(requireContext(), "No app data found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun createButtons(appList: List<AppData>) {
        val layout = view?.findViewById<LinearLayout>(R.id.buttonContainer)

        // Clear previous views
        layout?.removeAllViews()

        // Create a container for the buttons
        var rowLayout: LinearLayout? = null

        // Create a button for each app
        for (index in appList.indices) {
            val app = appList[index]

            // Create a new row every 2 buttons
            if (index % 2 == 0) {
                rowLayout = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                layout?.addView(rowLayout) // Add the new row to the main layout
            }

            // Create the button layout with background and padding
            val buttonLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, // Width will be calculated
                    LinearLayout.LayoutParams.WRAP_CONTENT // Height will be determined by contents
                ).apply {
                    weight = 1f // Equal width distribution
                    setMargins(10, 20, 10, 20) // Set margins for individual buttons
                }
                // Set the background and padding for individual buttons
                background = ContextCompat.getDrawable(requireActivity(), R.drawable.rounded_background_black)
                setPadding(50, 25, 50, 50)
            }

            // Create a TextView for the app name
            val appNameView = TextView(activity).apply {
                text = app.name
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER // Center the text
                setTextColor(ContextCompat.getColor(requireActivity(), android.R.color.white)) // Use a contrasting color
                textSize = 20f // Set the text size to make it bigger
                setTypeface(null, android.graphics.Typeface.BOLD) // Make the text bold
            }

            // Create an ImageView for the app icon
            val appIconView = ImageView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, // Set width to MATCH_PARENT
                    ViewGroup.LayoutParams.WRAP_CONTENT // Set height to WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 0) // Set top margin
                    gravity = Gravity.CENTER // Center the icon in the button
                }

                // Load the icon using Glide with fitCenter to maintain aspect ratio
                Glide.with(requireActivity())
                    .load(app.appIconUrl)
                    .fitCenter() // Maintain the aspect ratio
                    .into(this)
            }

            // Add the TextView and ImageView to the button layout
            buttonLayout.addView(appNameView)
            buttonLayout.addView(appIconView)

            // Create the action button below the icon
            val actionButton = Button(activity).apply {
                // Determine the action text
                val isInstalled = isAppInstalled(app.packageName)
                val updateAvailable = isUpdateAvailable(app.packageName, app.version)

                Log.d("AppCheck", "App: ${app.name}, Installed: $isInstalled, Update Available: $updateAvailable")

                val actionText = when {
                    isInstalled && updateAvailable -> {
                        Log.d("ButtonLogic", "App ${app.name} is installed and has an update available.")
                        "Update"
                    }
                    isInstalled -> {
                        Log.d("ButtonLogic", "App ${app.name} is installed and no updates are available.")
                        "Installed"
                    }
                    else -> {
                        Log.d("ButtonLogic", "App ${app.name} is not installed.")
                        "Install"
                    }
                }

                text = actionText
                background = ContextCompat.getDrawable(requireActivity(),
                    R.drawable.button_gradient_background
                )
                setTextColor(ContextCompat.getColor(requireActivity(), android.R.color.white))

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 0) // Add margin above the button (top margin)
                }

                setOnClickListener {
                    // Logic for installing or updating the app
                    handleButtonClick(app)
                }
            }

            // Add action button to the button layout
            buttonLayout.addView(actionButton)

            // Finally, add the button layout to the current row
            rowLayout?.addView(buttonLayout)
        }
    }



    private fun isAppInstalled(packageName: String): Boolean {
        Log.d("AppCheck", "Checking if package $packageName is installed.")
        return try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(packageName, 0)
            Log.d("AppCheck", "Package $packageName is installed, version: ${packageInfo.versionName}")
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("AppCheck", "Package $packageName is not installed.")
            false
        }
    }

    private fun isUpdateAvailable(packageName: String, newVersion: String): Boolean {
        return try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(packageName, 0)
            val installedVersion = packageInfo.versionName
            Log.d("AppCheck", "Installed version of $packageName: $installedVersion, New version: $newVersion")
            installedVersion < newVersion
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("AppCheck", "App with package name $packageName is not installed, so no update needed.")
            false
        }
    }

    private fun handleButtonClick(app: AppData) {
        if (isAppInstalled(app.packageName)) {
            // Logic for updating the app or showing "No updates available"
            if (isUpdateAvailable(app.packageName, app.version)) {
                downloadAndInstallApk(app.apkUrl)
            } else {
                Toast.makeText(activity, "No updates available", Toast.LENGTH_SHORT).show()
            }
        } else {
            downloadAndInstallApk(app.apkUrl)
        }
    }

    private fun downloadAndInstallApk(apkUrl: String) {
        Thread {
            try {
                val request = Request.Builder().url(apkUrl).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream()
                    val file = File(requireActivity().getExternalFilesDir(null), "downloaded_app.apk")
                    val outputStream = FileOutputStream(file)

                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }

                    val apkUri = FileProvider.getUriForFile(requireActivity(), "${requireActivity().packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant temporary read permission to the content URI
                    }
                    startActivity(intent)

                } else {
                    Log.e("HomeFragment", "Failed to download APK: ${response.message}")
                    activity?.runOnUiThread {
                        Toast.makeText(activity, "Download failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error downloading APK: ${e.message}")
                activity?.runOnUiThread {
                    Toast.makeText(activity, "Download error: ${e.message}", Toast.LENGTH_SHORT).show()
                    e.message?.let { Log.d("Error", it) }
                }
            }
        }.start()
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
        } else {
            if (!canInstallApps()) {
                showInstallUnknownAppsDialog()
            }
        }
    }

    private fun canInstallApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    private fun showInstallUnknownAppsDialog() {
        val dialog = Dialog(requireActivity())
        dialog.setContentView(R.layout.dialog_permission)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val buttonEnable: Button = dialog.findViewById(R.id.button_enable)

        buttonEnable.setOnClickListener {
            openInstallUnknownAppsSettings()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openInstallUnknownAppsSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${requireActivity().packageName}")
            }
        } else {
            // For Android versions below Oreo
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${requireActivity().packageName}")
            }
        }
        startActivity(intent)
    }
}
