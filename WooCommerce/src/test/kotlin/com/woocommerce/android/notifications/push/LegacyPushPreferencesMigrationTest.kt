package com.woocommerce.android.notifications.push

import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LegacyPushPreferencesMigrationTest : BaseUnitTest() {
    private val defaultPreferences: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()

    @Test
    fun `given only legacy token remains, when checking migration, then runs cleanup`() = testBlocking {
        whenever(defaultPreferences.contains(LEGACY_TOKEN_KEY)).thenReturn(true)

        assertThat(LegacyPushPreferencesMigration(defaultPreferences).shouldMigrate(preferencesOf())).isTrue()
    }

    @Test
    fun `given legacy UUID, when migrating, then preserves the existing push registration data`() =
        testBlocking {
            whenever(defaultPreferences.contains(LEGACY_UUID_KEY)).thenReturn(true)
            val currentData = preferencesOf(
                pushTokenIdKey(SITE_ID) to "registration-id",
                pushTokenValueKey(SITE_ID) to "fcm-token",
                pushLocaleKey(SITE_ID) to "en_US",
                pushIdentityKey(SITE_ID) to "installation-uuid",
                stringPreferencesKey("unrelated") to "value",
                booleanPreferencesKey("push_identity_full_registration_pending") to false
            )
            val migration = LegacyPushPreferencesMigration(defaultPreferences)

            assertThat(migration.shouldMigrate(currentData)).isTrue()

            val result = migration.migrate(currentData)

            assertThat(result).isSameAs(currentData)
            assertThat(result.asMap()).containsAllEntriesOf(currentData.asMap())
        }

    @Test
    fun `given legacy UUID, when cleanup succeeds, then removes only the legacy push keys`() = testBlocking {
        whenever(defaultPreferences.edit()).thenReturn(editor)
        whenever(editor.remove(LEGACY_UUID_KEY)).thenReturn(editor)
        whenever(editor.remove(LEGACY_TOKEN_KEY)).thenReturn(editor)
        whenever(editor.commit()).thenReturn(true)

        LegacyPushPreferencesMigration(defaultPreferences).cleanUp()

        verify(editor).remove(LEGACY_UUID_KEY)
        verify(editor).remove(LEGACY_TOKEN_KEY)
        verify(editor).commit()
    }

    @Test
    fun `given no legacy UUID, when checking migration, then does not migrate`() = testBlocking {
        whenever(defaultPreferences.contains(LEGACY_UUID_KEY)).thenReturn(false)

        val shouldMigrate = LegacyPushPreferencesMigration(defaultPreferences)
            .shouldMigrate(preferencesOf(stringPreferencesKey("unrelated") to "value"))

        assertThat(shouldMigrate).isFalse()
        verify(defaultPreferences, never()).edit()
    }

    @Test
    fun `given cleanup fails, when cleanup retries, then removes the legacy key`() = testBlocking {
        whenever(defaultPreferences.edit()).thenReturn(editor)
        whenever(editor.remove(LEGACY_UUID_KEY)).thenReturn(editor)
        whenever(editor.remove(LEGACY_TOKEN_KEY)).thenReturn(editor)
        whenever(editor.commit()).thenReturn(false, true)
        val migration = LegacyPushPreferencesMigration(defaultPreferences)

        val failure = runCatching { migration.cleanUp() }.exceptionOrNull()
        migration.cleanUp()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        verify(editor, times(2)).remove(LEGACY_UUID_KEY)
        verify(editor, times(2)).commit()
    }

    private companion object {
        const val SITE_ID = 123L
        const val LEGACY_TOKEN_KEY = "WC_PREF_NOTIFICATIONS_TOKEN"
        const val LEGACY_UUID_KEY = "WOO_CORE_PUSH_DEVICE_UUID"

        fun pushTokenIdKey(siteId: Long) = stringPreferencesKey("push_token_$siteId")
        fun pushTokenValueKey(siteId: Long) = stringPreferencesKey("push_token_value_$siteId")
        fun pushLocaleKey(siteId: Long) = stringPreferencesKey("push_locale_$siteId")
        fun pushIdentityKey(siteId: Long) = stringPreferencesKey("push_identity_$siteId")
    }
}
