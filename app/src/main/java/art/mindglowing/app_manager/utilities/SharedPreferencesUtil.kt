package art.mindglowing.app_manager.utilities
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SharedPreferencesUtil {

    private const val SHAREDPREFERENCESKEY = "MySettings"
    private const val LINKSKEY = "savedLinks"

    private val gson = Gson()

    fun getLinks(context: Context): List<String> {
        val sharedPreferences = context.getSharedPreferences(SHAREDPREFERENCESKEY, Context.MODE_PRIVATE)
        val existingLinksJson = sharedPreferences.getString(LINKSKEY, null)
        val type = object : TypeToken<List<String>>() {}.type
        return if (existingLinksJson != null) {
            gson.fromJson(existingLinksJson, type)
        } else {
            emptyList()
        }
    }
}
