package com.woocommerce.android.ui.prefs

import com.woocommerce.android.AppPrefsWrapper
import javax.inject.Inject

/**
 * Reconciles the local "Report Crashes" preference with the `woomobile_crash_reporting_opt_out`
 * WPCOM account setting after account settings are fetched:
 * - `true`/`false` on the account: the persisted choice wins and overwrites the local preference.
 * - `null` (no choice recorded yet): the local preference is the source of truth and is backfilled
 *   to the account, so existing opt-outs migrate up as users sign in.
 */
class CrashReportingSettingSync @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    private val repository: PrivacySettingsRepository,
) {
    suspend operator fun invoke() {
        when (val accountOptOut = repository.accountCrashReportingOptOut()) {
            null -> repository.updateCrashReportingSetting(enable = appPrefs.isCrashReportingEnabled())
            else -> appPrefs.setCrashReportingEnabled(!accountOptOut)
        }
    }
}
