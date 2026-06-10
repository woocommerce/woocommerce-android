package com.woocommerce.android.ui.prefs

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.ActivityAppSettingsBinding
import com.woocommerce.android.extensions.doOnApplyWindowInsets
import com.woocommerce.android.notifications.push.NotificationMessageHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.appwidgets.WidgetUpdater
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.designsystem.DesignSystemMode
import com.woocommerce.android.ui.designsystem.defaultDesignSystemMode
import com.woocommerce.android.ui.designsystem.xml.applyDesignSystemToolbarLayout
import com.woocommerce.android.ui.designsystem.xml.designSystemToolbarLayoutInflater
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.prefs.MainSettingsFragment.AppSettingsListener
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.util.parcelable
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.android.DispatchingAndroidInjector
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginMode
import javax.inject.Inject

@AndroidEntryPoint
class AppSettingsActivity :
    AppCompatActivity(),
    AppSettingsListener,
    AppSettingsContract.View {
    companion object {
        const val EXTRA_SHOW_PRIVACY_SETTINGS = "extra_show_privacy_settings"
        const val EXTRA_REQUESTED_ANALYTICS_VALUE_FROM_ERROR = "extra_requested_analytics_value_from_error"
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
    private var progressDialog: CustomProgressDialog? = null

    private lateinit var binding: ActivityAppSettingsBinding
    private var toolbar: Toolbar? = null
    private var toolbarDivider: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        val designSystemMode = defaultDesignSystemMode()
        val toolbarView = inflateToolbar(designSystemMode)
        toolbar = toolbarView
        binding.appBarLayout.addView(toolbarView)
        toolbarDivider = createToolbarDivider(designSystemMode)
        toolbarDivider?.let(binding.appBarLayout::addView)
        setContentView(binding.root)

        binding.root.doOnApplyWindowInsets(
            insetsMask = WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or
                WindowInsetsCompat.Type.ime(),
            consumeInsets = true
        ) {
            binding.root.updatePadding(
                left = it.left,
                right = it.right,
                bottom = it.bottom
            )
            binding.appBarLayout.updatePadding(
                top = it.top
            )
        }

        presenter.takeView(this)

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

        if (intent.getBooleanExtra(EXTRA_SHOW_PRIVACY_SETTINGS, false)) {
            val requestedAnalyticsValue =
                intent.parcelable(EXTRA_REQUESTED_ANALYTICS_VALUE_FROM_ERROR)
                    ?: RequestedAnalyticsValue.NONE

            navHostFragment.navController.navigate(
                MainSettingsFragmentDirections.actionMainSettingsFragmentToPrivacySettingsFragment(
                    requestedAnalyticsValue
                )
            )
        }
    }

    private val fragmentLifecycleObserver: FragmentLifecycleCallbacks = object : FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            if (f is DialogFragment) return

            when ((f as? BaseFragment)?.activityAppBarStatus ?: AppBarStatus.Visible()) {
                AppBarStatus.Hidden -> {
                    toolbar?.isVisible = false
                    toolbarDivider?.isVisible = false
                }
                is AppBarStatus.Visible -> {
                    toolbar?.isVisible = true
                    toolbarDivider?.isVisible = true
                }
            }
        }
    }

    private fun inflateToolbar(mode: DesignSystemMode): Toolbar {
        val toolbarInflater = designSystemToolbarLayoutInflater(layoutInflater, mode)
        return (toolbarInflater.inflate(R.layout.view_toolbar, binding.appBarLayout, false) as Toolbar).apply {
            applyDesignSystemToolbarLayout(mode)
        }
    }

    private fun createToolbarDivider(mode: DesignSystemMode): View? {
        if (mode == DesignSystemMode.LEGACY) return null

        return View(this).apply {
            layoutParams = AppBarLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.design_system_toolbar_divider_height)
            )
            setBackgroundColor(ContextCompat.getColor(context, R.color.design_system_outline_variant))
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroy() {
        hideLogoutProgressDialog()
        presenter.dropView(this)
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

    override fun finishLogout() {
        hideLogoutProgressDialog()
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()
        statsWidgetUpdaters.updateTodayWidget()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            LoginMode.WOO_LOGIN_MODE.putInto(this)
        }

        startActivity(intent)
        finish()
    }

    override fun showLogoutProgressDialog() {
        if (progressDialog != null) return

        (supportFragmentManager.findFragmentByTag(CustomProgressDialog.TAG) as? CustomProgressDialog)
            ?.dismissAllowingStateLoss()

        progressDialog = CustomProgressDialog.show(
            "",
            getString(R.string.settings_logout_dialog_message)
        ).also {
            it.isCancelable = false
            it.show(supportFragmentManager, CustomProgressDialog.TAG)
        }
    }

    override fun hideLogoutProgressDialog() {
        val dialog = progressDialog
            ?: supportFragmentManager.findFragmentByTag(CustomProgressDialog.TAG) as? CustomProgressDialog
        dialog?.dismissAllowingStateLoss()
        progressDialog = null
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
