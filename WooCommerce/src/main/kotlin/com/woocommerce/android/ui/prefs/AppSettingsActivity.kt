package com.woocommerce.android.ui.prefs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.ActivityAppSettingsBinding
import com.woocommerce.android.notifications.push.NotificationMessageHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.appwidgets.WidgetUpdater
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.prefs.MainSettingsFragment.AppSettingsListener
import com.woocommerce.android.util.AnalyticsUtils
import dagger.android.DispatchingAndroidInjector
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppSettingsActivity :
    AppCompatActivity(),
    AppSettingsListener,
    AppSettingsContract.View {
    companion object {
        const val RESULT_CODE_BETA_OPTIONS_CHANGED = 2
        const val KEY_BETA_OPTION_CHANGED = "key_beta_option_changed"
    }

    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var presenter: AppSettingsContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var prefs: AppPrefs
    @Inject lateinit var notificationMessageHandler: NotificationMessageHandler
    @Inject lateinit var statsWidgetUpdaters: WidgetUpdater.StatsWidgetUpdaters

    private var isBetaOptionChanged = false

    private lateinit var binding: ActivityAppSettingsBinding
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter.takeView(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHostFragment.childFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleObserver, false)

        savedInstanceState?.let {
            isBetaOptionChanged = it.getBoolean(KEY_BETA_OPTION_CHANGED)
        }

        if (isBetaOptionChanged) {
            setResult(RESULT_CODE_BETA_OPTIONS_CHANGED)
        }
    }

    private val fragmentLifecycleObserver: FragmentLifecycleCallbacks = object : FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            when ((f as? BaseFragment)?.activityAppBarStatus ?: AppBarStatus.Visible()) {
                AppBarStatus.Hidden -> {
                    toolbar?.isVisible = false
                }
                is AppBarStatus.Visible -> {
                    toolbar?.isVisible = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_BETA_OPTION_CHANGED, isBetaOptionChanged)
        super.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean {
        AnalyticsTracker.trackBackPressed(this)
        return if (findNavController(R.id.nav_host_fragment).navigateUp()) {
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_24dp)
            true
        } else {
            finish()
            true
        }
    }

    override fun onRequestLogout() {
        confirmLogout()
    }

    override fun onProductAddonsOptionChanged(enabled: Boolean) {
        if (AppPrefs.isProductAddonsEnabled != enabled) {
            isBetaOptionChanged = true
            AppPrefs.isProductAddonsEnabled = enabled
            setResult(RESULT_CODE_BETA_OPTIONS_CHANGED)
        }
    }

    override fun onTapToPayOptionChanged(enabled: Boolean) {
        if (AppPrefs.isTapToPayEnabled != enabled) {
            isBetaOptionChanged = true
            AppPrefs.isTapToPayEnabled = enabled
            setResult(RESULT_CODE_BETA_OPTIONS_CHANGED)
        }
    }

    override fun finishLogout() {
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()
        statsWidgetUpdaters.updateTodayWidget()

        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(mainIntent)
        setResult(Activity.RESULT_OK)

        close()
    }

    override fun close() {
        finish()
    }

    override fun confirmLogout() {
        val message = when (selectedSite.connectionType) {
            SiteConnectionType.ApplicationPasswords -> getString(R.string.settings_confirm_logout_site_credentials)
            else -> getString(R.string.settings_confirm_logout, presenter.getAccountDisplayName())
        }

        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setPositiveButton(R.string.signout) { _, _ ->
                AnalyticsTracker.track(
                    AnalyticsEvent.SETTINGS_LOGOUT_CONFIRMATION_DIALOG_RESULT,
                    mapOf(
                        AnalyticsTracker.KEY_RESULT to AnalyticsUtils.getConfirmationResultLabel(true)
                    )
                )

                presenter.logout()
            }
            .setNegativeButton(R.string.back) { _, _ ->
                AnalyticsTracker.track(
                    AnalyticsEvent.SETTINGS_LOGOUT_CONFIRMATION_DIALOG_RESULT,
                    mapOf(
                        AnalyticsTracker.KEY_RESULT to AnalyticsUtils.getConfirmationResultLabel(false)
                    )
                )
            }
            .setCancelable(true)
            .create()
            .show()
    }
}
