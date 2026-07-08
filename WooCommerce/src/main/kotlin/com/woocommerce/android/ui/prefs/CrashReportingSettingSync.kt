package com.woocommerce.android.ui.prefs

import com.woocommerce.android.AppPrefsWrapper
import javax.inject.Inject

/**
 * Reconciles the local "Report Crashes" preference with the `woomobile_crash_reporting_opt_out`
 * WPCOM account setting after account settings are fetched:
 * - `true`/`false` on the account: the persisted choice wins and overwrites the local preference.
 * - `null` (no choice recorded yet): if the user has actually made a local choice it is backfilled
 *   to the account, so existing opt-outs migrate up as users sign in. When the user never touched
 *   the setting, the local value is only the build-variant default, so nothing is pushed and the
 *   account stays `null` — preserving the endpoint's three-state "never chose" semantics.
 */
class CrashReportingSettingSync @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    private val repository: PrivacySettingsRepository,
) {
    suspend operator fun invoke() {
        when (val accountOptOut = repository.accountCrashReportingOptOut()) {
            null -> if (appPrefs.hasCrashReportingChoice()) {
                repository.updateCrashReportingSetting(enable = appPrefs.isCrashReportingEnabled())
            }
            else -> appPrefs.setCrashReportingEnabled(!accountOptOut)
        }
    }
}
