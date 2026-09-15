package com.woocommerce.android

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppPrefsPushIdentityTest {
    private lateinit var context: Context
    private lateinit var defaultPreferences: SharedPreferences
    private lateinit var excludedPreferences: SharedPreferences

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        excludedPreferences = context.getSharedPreferences(
            "${context.packageName}_deletable_preferences",
            Context.MODE_PRIVATE,
        )
        defaultPreferences.edit().clear().apply()
        excludedPreferences.edit().clear().apply()
    }

    @Test
    fun `given backed up push identity, when preferences are initialized, then ignore and remove restored values`() {
        defaultPreferences.edit()
            .putString(PUSH_DEVICE_UUID_KEY, RESTORED_UUID)
            .putString(FCM_TOKEN_KEY, RESTORED_TOKEN)
            .apply()

        AppPrefs.init(context)

        assertThat(AppPrefs.wooCorePushDeviceUUID)
            .isNotBlank()
            .isNotEqualTo(RESTORED_UUID)
        assertThat(AppPrefs.getFCMToken()).isEmpty()
        assertThat(excludedPreferences.getString(PUSH_DEVICE_UUID_KEY, null))
            .isEqualTo(AppPrefs.wooCorePushDeviceUUID)
        assertThat(defaultPreferences.contains(PUSH_DEVICE_UUID_KEY)).isFalse()
        assertThat(defaultPreferences.contains(FCM_TOKEN_KEY)).isFalse()
    }

    @Test
    fun `given push identity, when values are written and preferences reinitialized, then persist only in excluded preferences`() {
        AppPrefs.init(context)
        AppPrefs.wooCorePushDeviceUUID = CURRENT_UUID
        AppPrefs.setFCMToken(CURRENT_TOKEN)

        assertThat(excludedPreferences.getString(PUSH_DEVICE_UUID_KEY, null)).isEqualTo(CURRENT_UUID)
        assertThat(excludedPreferences.getString(FCM_TOKEN_KEY, null)).isEqualTo(CURRENT_TOKEN)
        assertThat(defaultPreferences.contains(PUSH_DEVICE_UUID_KEY)).isFalse()
        assertThat(defaultPreferences.contains(FCM_TOKEN_KEY)).isFalse()

        defaultPreferences.edit()
            .putString(PUSH_DEVICE_UUID_KEY, RESTORED_UUID)
            .putString(FCM_TOKEN_KEY, RESTORED_TOKEN)
            .apply()
        AppPrefs.init(context)

        assertThat(AppPrefs.wooCorePushDeviceUUID).isEqualTo(CURRENT_UUID)
        assertThat(AppPrefs.getFCMToken()).isEqualTo(CURRENT_TOKEN)
    }

    @Test
    fun `given push identity and app version, when logging out, then reset UUID and preserve token and app version`() {
        AppPrefs.init(context)
        AppPrefs.wooCorePushDeviceUUID = CURRENT_UUID
        AppPrefs.setFCMToken(CURRENT_TOKEN)
        AppPrefs.setLastAppVersionCode(APP_VERSION_CODE)

        AppPrefs.resetUserPreferences()

        assertThat(AppPrefs.wooCorePushDeviceUUID).isEmpty()
        assertThat(AppPrefs.getFCMToken()).isEqualTo(CURRENT_TOKEN)
        assertThat(AppPrefs.getLastAppVersionCode()).isEqualTo(APP_VERSION_CODE)
    }

    @Test
    fun `given push identity, when switching sites, then preserve UUID and token`() {
        AppPrefs.init(context)
        AppPrefs.wooCorePushDeviceUUID = CURRENT_UUID
        AppPrefs.setFCMToken(CURRENT_TOKEN)

        AppPrefs.resetSitePreferences()

        assertThat(AppPrefs.wooCorePushDeviceUUID).isEqualTo(CURRENT_UUID)
        assertThat(AppPrefs.getFCMToken()).isEqualTo(CURRENT_TOKEN)
    }

    private companion object {
        const val PUSH_DEVICE_UUID_KEY = "WOO_CORE_PUSH_DEVICE_UUID"
        const val FCM_TOKEN_KEY = "WC_PREF_NOTIFICATIONS_TOKEN"
        const val CURRENT_UUID = "current-uuid"
        const val CURRENT_TOKEN = "current-token"
        const val RESTORED_UUID = "restored-uuid"
        const val RESTORED_TOKEN = "restored-token"
        const val APP_VERSION_CODE = 42
    }
}
