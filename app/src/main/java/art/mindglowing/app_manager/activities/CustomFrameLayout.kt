package art.mindglowing.app_manager.activities

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

class CustomFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val extendedTouchArea = 1000 // Extend touch area by 1000 pixels
    private var initialX = 0f
    private var isIntercepted = false

    private var drawerLayout: DrawerLayout? = null // Reference to the DrawerLayout

    fun setDrawerLayout(drawerLayout: DrawerLayout) {
        this.drawerLayout = drawerLayout
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x // Store the initial touch position
                isIntercepted = false // Reset interception state
            }
            MotionEvent.ACTION_MOVE -> {
                val diffX = ev.x - initialX
                Log.d("CustomFrameLayout", "diffX: $diffX") // Log the swipe amount

                // Check if the swipe is to the right and within the extended area
                if (diffX > 100 && ev.x < extendedTouchArea) {
                    Log.d("CustomFrameLayout", "Intercepting swipe right") // Log when intercepting
                    isIntercepted = true // Set interception state
                    drawerLayout?.let { // If drawer layout is set, open it
                        if (!it.isDrawerOpen(GravityCompat.START)) {
                            it.openDrawer(GravityCompat.START)
                        }
                    }
                    return true // Intercept the touch event if it's a swipe right
                }
            }
        }

        // Allow other events to be processed normally if not intercepted
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Call performClick if a click is detected
        if (event.action == MotionEvent.ACTION_UP && !isIntercepted) {
            performClick()
        }

        if (isIntercepted) {
            // If the swipe was intercepted, we can allow the gesture detection to handle it
            return true // Return true to indicate that the event is handled
        }
        // Handle other touch events normally
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick() // Call the superclass method
        // Any additional click handling logic can go here
        return true // Indicate that the click was handled
    }
}