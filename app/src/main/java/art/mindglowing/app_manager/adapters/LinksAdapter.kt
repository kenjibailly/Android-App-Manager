package art.mindglowing.app_manager.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import art.mindglowing.app_manager.R

class LinksAdapter(
    private var links: List<String>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<LinksAdapter.LinkViewHolder>() {

    inner class LinkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val linkTextView: TextView = itemView.findViewById(R.id.linkTextView)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        fun bind(link: String) {
            // Use the new function to extract and capitalize the app name
            linkTextView.text = extractAndCapitalizeAppName(link)
            deleteButton.setOnClickListener { onDeleteClick(link) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_link, parent, false)
        return LinkViewHolder(view)
    }

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        holder.bind(links[position])
    }

    override fun getItemCount(): Int = links.size

    fun updateLinks(newLinks: List<String>) {
        links = newLinks
        notifyDataSetChanged()
    }

    // Function to extract and capitalize the app name
    private fun extractAndCapitalizeAppName(url: String): String {
        val lastSegment = url.substringAfterLast("/") // Get the last segment (apps.json)
        return lastSegment.substringBeforeLast(".").replaceFirstChar { it.uppercase() } // Remove .json and capitalize
    }
}
