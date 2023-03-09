package com.woocommerce.android.apifaker

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnLayout
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiFakerUiHelper @Inject constructor() : ActivityLifecycleCallbacks {
    @Inject
    internal lateinit var apiFakerConfig: ApiFakerConfig

    private val apiFakerHintId = View.generateViewId()

    fun attachToApplication(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        (activity as? ComponentActivity)?.lifecycleScope?.launch {
            updateApiFakerHint(WeakReference(activity))
        }
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    private suspend fun updateApiFakerHint(
        activityReference: WeakReference<ComponentActivity>
    ) {
        apiFakerConfig.enabled.collect { enabled ->
            activityReference.get()?.let { activity ->
                if (enabled) {
                    activity.window.decorView.post {
                        activity.window.decorView.showApiFakerHint(activity)
                    }
                } else {
                    activity.window.decorView.post {
                        activity.window.decorView.hideApiFakerHint()
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun View.showApiFakerHint(activity: ComponentActivity) {
        fun dpToPx(dp: Int): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                resources.displayMetrics
            ).toInt()
        }
        // This works only for activities that has the content view as a direct child of the FrameLayout, which is true
        // for all AppCompat activities, so it should work for all the cases we need.
        val contentLayout = findViewById<View>(android.R.id.content) as? FrameLayout ?: return
        val activityLayout = contentLayout.getChildAt(0)

        val apiFakerHint = FrameLayout(context).apply {
            id = apiFakerHintId
            setBackgroundColor(Color.RED)
            setPadding(dpToPx(4))
            addView(
                TextView(context).apply {
                    text = "ApiFaker Enabled"
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
            )
            setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("ApiFaker")
                    .setMessage("ApiFaker is enabled. Do you want to disable it?")
                    .setPositiveButton("Yes") { _, _ ->
                        activity.lifecycleScope.launch {
                            apiFakerConfig.setStatus(false)
                        }
                    }
                    .setNegativeButton("No") { _, _ -> }
                    .show()
            }
        }
        contentLayout.addView(
            apiFakerHint,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM
            }
        )

        apiFakerHint.doOnLayout { view ->
            activityLayout.updateLayoutParams<MarginLayoutParams> { bottomMargin = view.measuredHeight }
        }
    }

    private fun View.hideApiFakerHint() {
        val contentLayout = findViewById<ViewGroup>(android.R.id.content)
        val activityLayout = contentLayout.getChildAt(0)

        contentLayout.findViewById<View>(apiFakerHintId)?.let { apiFakerHint ->
            contentLayout.removeView(apiFakerHint)
            activityLayout.updateLayoutParams<MarginLayoutParams> { bottomMargin = 0 }
        }
    }
}
