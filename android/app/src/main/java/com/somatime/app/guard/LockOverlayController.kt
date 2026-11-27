package com.somatime.app.guard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.somatime.app.MainActivity

class LockOverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var topSwipeBlockerView: View? = null
    private var fullLockOverlayView: View? = null

    fun showTopSwipeBlocker() {
        if (!UsageAccessHelper.canDrawOverlays(context)) return
        mainHandler.post {
            if (topSwipeBlockerView != null) return@post
            val heightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                72f,
                context.resources.displayMetrics
            ).toInt()

            val view = View(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                setOnTouchListener { _, _ -> true }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                heightPx,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            runCatching {
                windowManager.addView(view, params)
                topSwipeBlockerView = view
            }
        }
    }

    fun hideTopSwipeBlocker() {
        mainHandler.post {
            val view = topSwipeBlockerView ?: return@post
            runCatching { windowManager.removeViewImmediate(view) }
            topSwipeBlockerView = null
        }
    }

    fun showFullLockOverlay() {
        if (!UsageAccessHelper.canDrawOverlays(context)) return
        mainHandler.post {
            if (fullLockOverlayView != null) return@post

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#F50F172A"))
                val pad = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    28f,
                    context.resources.displayMetrics
                ).toInt()
                setPadding(pad, pad, pad, pad)
                isFocusable = true
                isFocusableInTouchMode = true
                setOnKeyListener { _, keyCode, _ ->
                    keyCode == KeyEvent.KEYCODE_BACK ||
                        keyCode == KeyEvent.KEYCODE_HOME ||
                        keyCode == KeyEvent.KEYCODE_APP_SWITCH ||
                        keyCode == KeyEvent.KEYCODE_MENU
                }
            }

            val badge = TextView(context).apply {
                text = "SOMATIME SCREEN GUARD ACTIVE"
                setTextColor(Color.parseColor("#F59E0B"))
                textSize = 13f
                gravity = Gravity.CENTER
            }

            val title = TextView(context).apply {
                text = "Screen Time Expired (00:00)"
                setTextColor(Color.WHITE)
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(0, 24, 0, 12)
            }

            val subtitle = TextView(context).apply {
                text = "Other apps, settings, and notifications are locked. Complete a Kenyan CBC activity or enter Parent PIN to unlock."
                setTextColor(Color.parseColor("#CBD5E1"))
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 40)
            }

            val actionBtn = Button(context).apply {
                text = "Open CBC Lesson / Parent PIN"
                setBackgroundColor(Color.parseColor("#059669"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        )
                    }
                    runCatching { context.startActivity(intent) }
                }
            }

            container.addView(badge)
            container.addView(title)
            container.addView(subtitle)
            container.addView(actionBtn)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            runCatching {
                windowManager.addView(container, params)
                fullLockOverlayView = container
                container.requestFocus()
            }
        }
    }

    fun hideFullLockOverlay() {
        mainHandler.post {
            val view = fullLockOverlayView ?: return@post
            runCatching { windowManager.removeViewImmediate(view) }
            fullLockOverlayView = null
        }
    }

    fun hideAll() {
        hideFullLockOverlay()
        hideTopSwipeBlocker()
    }

    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
}
