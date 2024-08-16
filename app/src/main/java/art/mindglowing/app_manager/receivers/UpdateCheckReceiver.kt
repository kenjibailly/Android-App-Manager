package art.mindglowing.app_manager.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import art.mindglowing.app_manager.workers.UpdateCheckWorker

class UpdateCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Enqueue the UpdateCheckWorker
        val updateCheckRequest = OneTimeWorkRequestBuilder<UpdateCheckWorker>().build()
        WorkManager.getInstance(context).enqueue(updateCheckRequest)
    }
}
