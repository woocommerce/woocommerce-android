package com.woocommerce.android.apifaker

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.graphics.Color
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.withIndex
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

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private suspend fun updateApiFakerHint(
        activityReference: WeakReference<ComponentActivity>
    ) {
        apiFakerConfig.enabled.withIndex().collect { (index, enabled) ->
            activityReference.get()?.let { activity ->
                if (enabled) {
                    activity.window.decorView.post {
                        activity.window.decorView.showApiFakerHint(activity, animate = index != 0)
                    }
                } else {
                    activity.window.decorView.post {
                        activity.window.decorView.hideApiFakerHint(index != 0)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun View.showApiFakerHint(activity: ComponentActivity, animate: Boolean) {
        // This works only for activities that has the content view as a direct child of the FrameLayout, which is true
        // for all AppCompat activities, so it should work for all the cases we need.
        val contentLayout = findViewById<View>(android.R.id.content) as? FrameLayout ?: return
        val activityLayout = contentLayout.getChildAt(0) ?: return

        val apiFakerHint = FrameLayout(context).apply {
            id = apiFakerHintId
            setBackgroundColor(Color.RED)
            addView(
                TextView(context).apply {
                    text = "ApiFaker Enabled"
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTextColor(Color.WHITE)
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

        if (animate) {
            TransitionManager.beginDelayedTransition(contentLayout)
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
        apiFakerHint.handleBottomNavigationInset(activity)
    }

    private fun View.hideApiFakerHint(animate: Boolean) {
        val contentLayout = findViewById<ViewGroup>(android.R.id.content)
        val activityLayout = contentLayout.getChildAt(0) ?: return

        if (animate) {
            TransitionManager.beginDelayedTransition(contentLayout)
        }

        contentLayout.findViewById<View>(apiFakerHintId)?.let { apiFakerHint ->
            contentLayout.removeView(apiFakerHint)
        }
        activityLayout.updateLayoutParams<MarginLayoutParams> { bottomMargin = 0 }
    }

    private fun View.handleBottomNavigationInset(activity: Activity) {
        // setOnApplyWindowInsetsListener doesn't work for this case because the view is added after the window
        // insets are applied. So we need to handle it manually.
        activity.window.decorView.rootWindowInsets.let {
            val bottomNavigationInset = WindowInsetsCompat.toWindowInsetsCompat(it)
                .getInsets(WindowInsetsCompat.Type.navigationBars())
                .bottom
            updateLayoutParams<MarginLayoutParams> { bottomMargin = bottomNavigationInset }
        }
    }
}
