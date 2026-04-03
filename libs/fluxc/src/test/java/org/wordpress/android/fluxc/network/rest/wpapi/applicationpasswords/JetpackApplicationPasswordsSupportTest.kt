package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsSupport.Companion.FLAG_REFRESH_DURATION_DAYS
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsSupport.Companion.UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES
import org.wordpress.android.fluxc.utils.PreferenceUtils
import kotlin.time.Duration.Companion.days

@RunWith(RobolectricTestRunner::class)
class JetpackApplicationPasswordsSupportTest {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport

    private val testSiteId = 123L

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sharedPreferences = PreferenceUtils.getFluxCPreferences(context)
        jetpackApplicationPasswordsSupport = JetpackApplicationPasswordsSupport(context = context, appLog = mock())
    }

    @After
    fun tearDown() {
        sharedPreferences.edit().remove(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES).apply()
    }

    @Test
    fun `given site with no flag and app passwords supported, when checking support, then return true`() {
        // Given
        val testSite = createSite(supportsAppPasswords = true)
        // SharedPreferences is empty by default

        // When
        val result = jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `given site with no flag but app passwords not supported, when checking support, then return false`() {
        // Given
        val siteWithoutAppPasswordsSupport = createSite(supportsAppPasswords = false)

        // When
        val result = jetpackApplicationPasswordsSupport.supportsAppPasswords(siteWithoutAppPasswordsSupport)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `given site flagged as unsupported within refresh duration, when checking support, then return false`() {
        // Given
        val testSite = createSite(supportsAppPasswords = true)
        val currentTime = System.currentTimeMillis()
        val flagTime = currentTime - (FLAG_REFRESH_DURATION_DAYS.days - 1.days).inWholeMilliseconds
        val unsupportedSites = setOf("$testSiteId:$flagTime")

        // Pre-populate SharedPreferences with the flag
        sharedPreferences.edit()
            .putStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, unsupportedSites)
            .apply()

        // When
        val result = jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `given site flagged as unsupported beyond refresh duration, when checking support, then clear flag and return site support status`() {
        // Given
        val testSite = createSite(supportsAppPasswords = true)
        val currentTime = System.currentTimeMillis()
        val flagTime = currentTime - (FLAG_REFRESH_DURATION_DAYS.days + 1.days).inWholeMilliseconds
        val unsupportedSites = setOf("$testSiteId:$flagTime", "456:$currentTime")

        // Pre-populate SharedPreferences with the flags
        sharedPreferences.edit()
            .putStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, unsupportedSites)
            .apply()

        // When
        val result = jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)

        // Then
        assertThat(result).isTrue() // Returns site's isApplicationPasswordsSupported value

        // Verify that the expired flag was removed but the other flag remains
        val remainingFlags = sharedPreferences.getStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, null)
        assertThat(remainingFlags).containsExactly("456:$currentTime")
    }

    @Test
    fun `given multiple sites in unsupported list, when checking support for different site, then return site support status`() {
        // Given
        val differentSiteId = 999L
        val differentSite = createSite(siteId = differentSiteId, supportsAppPasswords = true)
        val currentTime = System.currentTimeMillis()
        val flagTime = currentTime - (FLAG_REFRESH_DURATION_DAYS.days - 1.days).inWholeMilliseconds
        val unsupportedSites = setOf("$testSiteId:$flagTime", "456:$currentTime")

        // Pre-populate SharedPreferences with the flags
        sharedPreferences.edit()
            .putStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, unsupportedSites)
            .apply()

        // When
        val result = jetpackApplicationPasswordsSupport.supportsAppPasswords(differentSite)

        // Then
        assertThat(result).isTrue()

        // Verify that the existing flags were not modified
        val remainingFlags = sharedPreferences.getStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, null)
        assertThat(remainingFlags).containsExactlyInAnyOrder("$testSiteId:$flagTime", "456:$currentTime")
    }

    @Test
    fun `given site not yet flagged, when flagging as unsupported, then add site to unsupported list`() {
        // Given
        val testSite = createSite()
        val existingUnsupportedSites = setOf("456:1234567890")

        // Pre-populate SharedPreferences with existing flags
        sharedPreferences.edit()
            .putStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, existingUnsupportedSites)
            .apply()

        // When
        jetpackApplicationPasswordsSupport.flagAsUnsupported(testSite)

        // Then
        val updatedFlags = sharedPreferences.getStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, null)
        assertThat(updatedFlags).hasSize(2)
        assertThat(updatedFlags).contains("456:1234567890")
        assertThat(updatedFlags!!.any { it.startsWith("$testSiteId:") }).isTrue()
    }

    @Test
    fun `given site already flagged, when flagging as unsupported again, then do not modify the list`() {
        // Given
        val testSite = createSite()
        val currentTime = System.currentTimeMillis()
        val existingUnsupportedSites = setOf("$testSiteId:$currentTime", "456:1234567890")

        // Pre-populate SharedPreferences with existing flags
        sharedPreferences.edit()
            .putStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, existingUnsupportedSites)
            .apply()

        // When
        jetpackApplicationPasswordsSupport.flagAsUnsupported(testSite)

        // Then
        val updatedFlags = sharedPreferences.getStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, null)
        assertThat(updatedFlags).isEqualTo(existingUnsupportedSites)
    }

    @Test
    fun `given malformed flag entry, when checking support, then handle gracefully`() {
        // Given
        val testSite = createSite(supportsAppPasswords = true)
        val unsupportedSites = setOf("$testSiteId:invalid_timestamp", "456:1234567890")
        sharedPreferences.edit()
            .putStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, unsupportedSites)
            .apply()

        // When
        val result = jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)

        // Then
        assertThat(result).isTrue()
        val remainingFlags = sharedPreferences.getStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, null)
        assertThat(remainingFlags).noneMatch { it.startsWith("$testSiteId:") }
    }

    @Test
    fun `given concurrent access, when flagging and checking support, then maintain data integrity`() = runTest {
        // Given
        val testSite = createSite()
        val currentTime = System.currentTimeMillis()
        val flagTime = currentTime - (FLAG_REFRESH_DURATION_DAYS + 1).days.inWholeMilliseconds
        sharedPreferences.edit()
            .putStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, setOf("$testSiteId:$flagTime"))
            .apply()

        // When
        val accessJobs = (0..4).map {
            launch(Dispatchers.Default) {
                jetpackApplicationPasswordsSupport.supportsAppPasswords(testSite)
            }
        }
        val updateJobs = (0..4).map {
            launch(Dispatchers.Default) {
                jetpackApplicationPasswordsSupport.flagAsUnsupported(testSite)
            }
        }
        joinAll(*accessJobs.toTypedArray(), *updateJobs.toTypedArray())

        // Then
        val updatedFlags = sharedPreferences.getStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, null)
        val siteFlag = updatedFlags?.firstOrNull { it.startsWith("$testSiteId:") }
        assertThat(siteFlag).isNotNull
        val timestamp = siteFlag!!.substringAfter(":").toLongOrNull()
        assertThat(timestamp).isGreaterThanOrEqualTo(currentTime)
    }

    private fun createSite(siteId: Long = testSiteId, supportsAppPasswords: Boolean = true): SiteModel {
        val site = SiteModel()
        site.siteId = siteId
        site.applicationPasswordsAuthorizeUrl = if (supportsAppPasswords) "https://example.com/authorize" else null
        return site
    }
}
