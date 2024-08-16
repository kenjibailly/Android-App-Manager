package art.mindglowing.app_manager.activities

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import art.mindglowing.app_manager.R
import art.mindglowing.app_manager.fragments.HomeFragment
import art.mindglowing.app_manager.fragments.SettingsFragment
import art.mindglowing.app_manager.receivers.UpdateCheckReceiver

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var openDrawerButton: LinearLayout
    private lateinit var fragmentNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize drawer layout and open drawer button
        drawerLayout = findViewById(R.id.drawer_layout)
        openDrawerButton = findViewById(R.id.open_drawer_button)
        fragmentNameTextView = findViewById(R.id.fragment_name)

        // Load HomeFragment as the default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            updateFragmentName("Home") // Set the initial fragment name
            highlightSelectedItem(R.id.nav_home) // Highlight the default item
        }

        // Enable the button to open the navigation drawer
        openDrawerButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        setupGestureDetection()

        // Set up navigation item selection for custom TextViews
        val homeTextView: TextView = findViewById(R.id.nav_home) // ID from your nav_drawer.xml
        val settingsTextView: TextView = findViewById(R.id.nav_settings) // ID from your nav_drawer.xml

        // Set click listeners for navigation items
        homeTextView.setOnClickListener {
            loadFragment(HomeFragment())
            updateFragmentName("Home") // Update fragment name
            highlightSelectedItem(R.id.nav_home) // Highlight Home item
            drawerLayout.closeDrawers()
        }

        settingsTextView.setOnClickListener {
            loadFragment(SettingsFragment())
            updateFragmentName("Settings") // Update fragment name
            highlightSelectedItem(R.id.nav_settings) // Highlight Settings item
            drawerLayout.closeDrawers()
        }

        checkNotificationPermission()
        createNotificationChannel()

        // Set the alarm for checking updates
        setUpdateAlarm()
    }

    private fun setupGestureDetection() {
        val customFrameLayout = findViewById<CustomFrameLayout>(R.id.fragment_container)
        customFrameLayout.setDrawerLayout(drawerLayout)
    }

    private fun highlightSelectedItem(selectedId: Int) {
        val homeTextView: TextView = findViewById(R.id.nav_home)
        val settingsTextView: TextView = findViewById(R.id.nav_settings)

        // Reset backgrounds to default
        homeTextView.background = null // or set to a transparent background
        settingsTextView.background = null // or set to a transparent background

        // Highlight the selected item with the rounded background
        when (selectedId) {
            R.id.nav_home -> homeTextView.setBackgroundResource(R.drawable.rounded_background_black)
            R.id.nav_settings -> settingsTextView.setBackgroundResource(R.drawable.rounded_background_black)
        }
    }

    private fun updateFragmentName(name: String) {
        fragmentNameTextView.text = name // Set the fragment name in the TextView
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment) // This ID should match the FrameLayout in your layout
        transaction.commit()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API level 33
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            } else {
                // Permission already granted
                createNotificationChannel() // Call your method to create the notification channel
            }
        } else {
            // For devices below API level 33, no need to request permission
            createNotificationChannel()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            101 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, create the notification channel
                    createNotificationChannel()
                } else {
                    // Permission denied, show a message to the user
                    Toast.makeText(this, "Notification permission is required to receive updates.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Update Channel"
            val descriptionText = "Notifications for app updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("update_channel", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setUpdateAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, UpdateCheckReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Set the alarm to trigger every 10 seconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 10000, pendingIntent) // Set to every 10 seconds
        } else {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent) // Fallback for older versions
        }
    }
}
