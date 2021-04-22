package com.woocommerce.android.util.crashlogging

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.locale.LocaleProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import java.util.Locale

@RunWith(MockitoJUnitRunner::class)
class WCCrashLoggingDataProviderTest {

    lateinit var sut: WCCrashLoggingDataProvider

    private val localeProvider: LocaleProvider = mock()
    private val accountStore: AccountStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val appPrefs: AppPrefs = mock()

    @Before
    fun setUp() {
        sut = WCCrashLoggingDataProvider(
            localeProvider = localeProvider,
            accountStore = accountStore,
            selectedSite = selectedSite,
            appPrefs = appPrefs
        )
    }

    @Test
    fun `should provide site id and site url in apps context if selected site exists`() {
        whenever(selectedSite.getIfExists()).thenReturn(TEST_SITE_MODEL)

        val appContext = sut.applicationContextProvider()

        assertThat(appContext).containsAllEntriesOf(
            mapOf(
                "site_id" to TEST_SITE_MODEL.siteId.toString(),
                "site_url" to TEST_SITE_MODEL.url
            )
        )
    }

    @Test
    fun `should provide empty apps context if selected site does not exist`() {
        whenever(selectedSite.getIfExists()).thenReturn(null)

        val appContext = sut.applicationContextProvider()

        assertThat(appContext).isEmpty()
    }

    @Test
    fun `should enable crash logging if crash logging is enabled`() {
        whenever(appPrefs.isCrashReportingEnabled()).thenReturn(true)

        val crashLoggingEnabled = sut.crashLoggingEnabled()

        assertThat(crashLoggingEnabled).isTrue
    }

    @Test
    fun `should disable crash logging if crash logging is disabled`() {
        whenever(appPrefs.isCrashReportingEnabled()).thenReturn(false)

        val crashLoggingEnabled = sut.crashLoggingEnabled()

        assertThat(crashLoggingEnabled).isFalse
    }

    @Test
    fun `should not include extra keys for events`() {
        assertThat(sut.extraKnownKeys()).isEmpty()
    }

    @Test
    fun `should provide correctly mapped user if user exists`() {
        whenever(accountStore.account).thenReturn(TEST_ACCOUNT)

        val user = sut.userProvider()

        SoftAssertions().apply {
            assertThat(user?.username).isEqualTo(TEST_ACCOUNT.userName)
            assertThat(user?.email).isEqualTo(TEST_ACCOUNT.email)
            assertThat(user?.userID).isEqualTo(TEST_ACCOUNT.userId.toString())
        }.assertAll()
    }

    @Test
    fun `should not provide user if user does not exist`() {
        whenever(accountStore.account).thenReturn(null)

        val user = sut.userProvider()

        assertThat(user).isNull()
    }

    @Test
    fun `should provide recent locale after locale change`() {
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

        assertThat(sut.locale).isEqualTo(Locale.US)

        whenever(localeProvider.provideLocale()).thenReturn(Locale.CANADA)

        assertThat(sut.locale).isEqualTo(Locale.CANADA)
    }

    companion object {
        val TEST_ACCOUNT = AccountModel().apply {
            userId = 123L
            email = "mail@a8c.com"
            userName = "username"
        }

        val TEST_SITE_MODEL = SiteModel().apply {
            siteId = 7L
            url = "automattic.com"
        }
    }
}
