package art.mindglowing.app_manager.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import art.mindglowing.app_manager.R
import art.mindglowing.app_manager.adapters.LinksAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class SettingsFragment : Fragment() {

    private lateinit var linkEditText: EditText
    private lateinit var addButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var linksAdapter: LinksAdapter
    private val client = OkHttpClient()

    private val sharedPreferencesKey = "MySettings"
    private val linksKey = "savedLinks"

    private fun isValidLink(link: String): Boolean {
        // Check if the link ends with .json and is a valid URL format
        return link.endsWith(".json") && android.util.Patterns.WEB_URL.matcher(link).matches()
    }

    private fun isValidJsonStructure(link: String, callback: (Boolean) -> Unit) {
        val request = Request.Builder().url(link).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false) // Notify failure in the callback
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val json = responseBody.string()
                    val gson = Gson()
                    val appType = object : TypeToken<List<AppData>>() {}.type

                    try {
                        val appDataList: List<AppData> = gson.fromJson(json, appType)

                        // Validate JSON structure based on the new AppData field names
                        val isValid = appDataList.all { appData ->
                            appData.name.isNotEmpty() &&
                                    appData.apkUrl.isNotEmpty() &&
                                    appData.appIconUrl.isNotEmpty() &&
                                    appData.version.isNotEmpty() &&
                                    appData.packageName.isNotEmpty()
                        }
                        callback(isValid) // Call back with the validity status
                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback(false) // Call back with false if parsing fails
                    }
                } ?: run {
                    callback(false) // Call back with false if body is null
                }
            }
        })
    }




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        linkEditText = view.findViewById(R.id.linkEditText)
        addButton = view.findViewById(R.id.addButton)
        recyclerView = view.findViewById(R.id.recyclerView)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        linksAdapter = LinksAdapter(getLinks()) { link -> deleteLink(link) }
        recyclerView.adapter = linksAdapter

        addButton.setOnClickListener {
            val link = linkEditText.text.toString()
            if (link.isNotEmpty()) {
                if (isValidLink(link)) {
                    // Validate JSON structure
                    isValidJsonStructure(link) { isValid ->
                        requireActivity().runOnUiThread {
                            if (isValid) {
                                addLink(link)
                                linkEditText.text.clear() // Clear the input after adding
                            } else {
                                Toast.makeText(requireContext(), "Invalid JSON structure", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid JSON link", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a link", Toast.LENGTH_SHORT).show()
            }
        }


        return view
    }

    private fun addLink(link: String) {
        val sharedPreferences = requireContext().getSharedPreferences(sharedPreferencesKey, Context.MODE_PRIVATE)
        val gson = Gson()

        // Get the existing links or initialize a new list
        val existingLinksJson = sharedPreferences.getString(linksKey, null)
        val type = object : TypeToken<MutableList<String>>() {}.type
        val linksList: MutableList<String> = if (existingLinksJson != null) {
            gson.fromJson(existingLinksJson, type)
        } else {
            mutableListOf()
        }

        // Add the new link to the list and save it back to shared preferences
        linksList.add(link)
        val newLinksJson = gson.toJson(linksList)

        sharedPreferences.edit().putString(linksKey, newLinksJson).apply()
        Toast.makeText(requireContext(), "Link added", Toast.LENGTH_SHORT).show()

        // Update the adapter
        linksAdapter.updateLinks(getLinks())
    }

    private fun deleteLink(link: String) {
        val sharedPreferences = requireContext().getSharedPreferences(sharedPreferencesKey, Context.MODE_PRIVATE)
        val gson = Gson()

        // Get the existing links
        val existingLinksJson = sharedPreferences.getString(linksKey, null)
        val type = object : TypeToken<MutableList<String>>() {}.type
        val linksList: MutableList<String> = if (existingLinksJson != null) {
            gson.fromJson(existingLinksJson, type)
        } else {
            mutableListOf()
        }

        // Remove the link
        linksList.remove(link)
        val newLinksJson = gson.toJson(linksList)

        sharedPreferences.edit().putString(linksKey, newLinksJson).apply()
        Toast.makeText(requireContext(), "Link deleted", Toast.LENGTH_SHORT).show()

        // Update the adapter
        linksAdapter.updateLinks(getLinks())
    }

    private fun getLinks(): List<String> {
        val sharedPreferences = requireContext().getSharedPreferences(sharedPreferencesKey, Context.MODE_PRIVATE)
        val gson = Gson()
        val existingLinksJson = sharedPreferences.getString(linksKey, null)
        val type = object : TypeToken<MutableList<String>>() {}.type
        return if (existingLinksJson != null) {
            gson.fromJson(existingLinksJson, type)
        } else {
            emptyList()
        }
    }
}
