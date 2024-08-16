package art.mindglowing.app_manager.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Log
import art.mindglowing.app_manager.R
import art.mindglowing.app_manager.activities.MainActivity
import art.mindglowing.app_manager.fragments.AppData
import art.mindglowing.app_manager.utilities.SharedPreferencesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class UpdateCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val client = OkHttpClient()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("update_preferences", Context.MODE_PRIVATE)

    override fun doWork(): Result {
        val latestAppList = runBlocking {
            fetchLatestAppData() // Call the suspended function
        }

        for (app in latestAppList) {
            val lastNotifiedVersion = sharedPreferences.getString(app.packageName, null)
            if (isUpdateAvailable(app.packageName, app.version, lastNotifiedVersion)) {
                triggerUpdateNotification(app)
                // Update the last notified version in SharedPreferences
                sharedPreferences.edit().putString(app.packageName, app.version).apply()
            }
        }

        return Result.success()
    }

    private suspend fun fetchLatestAppData(): List<AppData> {
        val links = SharedPreferencesUtil.getLinks(applicationContext)
        if (links.isEmpty()) {
            Log.d("SettingsFragment", "No links found in shared preferences.")
        } else {
            Log.d("UpdateLinksList", links.toString())
        }

        // Use a mutable list to store app data from all links
        val allAppData = mutableListOf<AppData>()

        // Fetch data from all links concurrently
        coroutineScope {
            val deferredResults = links.map { link ->
                async(Dispatchers.IO) {
                    fetchAppDataFromLink(link)
                }
            }

            // Collect results from all async calls
            val results = deferredResults.awaitAll()

            for (appDataList in results) {
                allAppData.addAll(appDataList)
            }
        }

        // Log the size and contents after all calls
        Log.d("UpdateCheckWorker", "Fetched latest app list size: ${allAppData.size}")
        Log.d("UpdateCheckWorker", "Latest app list contents: $allAppData")

        return allAppData // Return the collected app data
    }

    private suspend fun fetchAppDataFromLink(link: String): List<AppData> {
        val request = Request.Builder()
            .url(link)
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            val json = response.body?.string()

            if (json != null) {
                val gson = Gson()
                val appType = object : TypeToken<List<AppData>>() {}.type
                val appData: List<AppData> = gson.fromJson(json, appType)
                Log.d("UpdateCheckWorker", "Received JSON: $json")
                Log.d("UpdateCheckWorker", "Parsed AppData: $appData")
                appData
            } else {
                listOf()
            }
        }
    }

    private fun triggerUpdateNotification(app: AppData) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = app.packageName.hashCode()

        val largeIconBitmap = BitmapFactory.decodeResource(applicationContext.resources,
            R.drawable.logo
        )

        // Create an intent to launch your main activity
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the task and start fresh
        }

        // Create a PendingIntent with FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Add FLAG_IMMUTABLE here
        )

        val notification = NotificationCompat.Builder(applicationContext, "update_channel")
            .setSmallIcon(R.drawable.ic_small_logo)
            .setLargeIcon(largeIconBitmap)
            .setContentTitle("Update Available")
            .setContentText("An update is available for ${app.name}. Version: ${app.version}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)  // Set the pending intent
            .setAutoCancel(true)  // Automatically remove the notification when tapped
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun isUpdateAvailable(packageName: String, newVersion: String, lastNotifiedVersion: String?): Boolean {
        Log.d("UpdateCheckWorker", "Checking update for package: $packageName")
        return try {
            val packageInfo = applicationContext.packageManager.getPackageInfo(packageName, 0)
            val installedVersion = packageInfo.versionName
            Log.d("UpdateCheckWorker", "Installed version: $installedVersion, New version: $newVersion, Last notified version: $lastNotifiedVersion")
            installedVersion < newVersion && newVersion != lastNotifiedVersion
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("UpdateCheckWorker", "Package not found: $packageName")
            false
        }
    }
}
