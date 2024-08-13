package com.woocommerce.android.wear.crashlogging

import android.content.Context
import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.ReleaseName
import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.wear.settings.SettingsRepository
import com.woocommerce.android.wear.ui.login.LoginRepository
import java.util.Locale
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore

@OptIn(ExperimentalCoroutinesApi::class)
class WCWearCrashLoggingDataProviderTest : BaseUnitTest() {
    private lateinit var sut: WCWearCrashLoggingDataProvider

    private val appContext: Context = mock()
    private val accountStore: AccountStore = mock()
    private val providedLocale: Locale = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val loginRepository: LoginRepository = mock()
    private val dispatcher: Dispatcher = mock()

    @Before
    fun setUp() {
        sut = WCWearCrashLoggingDataProvider(
            appScope = TestScope(coroutinesTestRule.testDispatcher),
            appContext = appContext,
            accountStore = accountStore,
            providedLocale = providedLocale,
            settingsRepository = settingsRepository,
            loginRepository = loginRepository,
            dispatcher = dispatcher
        )
    }

    private fun reinitialize() {
        setUp()
    }

    @Test
    fun `should provide empty apps context if selected site does not exist`() = testBlocking {
        whenever(loginRepository.selectedSiteFlow).thenReturn(MutableStateFlow(null))
        reinitialize()

        val appContext = sut.applicationContextProvider.first()

        assertThat(appContext).isEmpty()
    }

    @Test
    fun `should enable crash logging if crash logging is enabled`() {
        whenever(settingsRepository.crashReportEnabled.value).thenReturn(true)

        val crashLoggingEnabled = sut.crashLoggingEnabled()

        assertThat(crashLoggingEnabled).isTrue
    }

    @Test
    fun `should disable crash logging if crash logging is disabled`() {
        whenever(settingsRepository.crashReportEnabled.value).thenReturn(false)

        val crashLoggingEnabled = sut.crashLoggingEnabled()

        assertThat(crashLoggingEnabled).isFalse
    }

    @Test
    fun `should not include extra keys for events`() {
        assertThat(sut.extraKnownKeys()).isEmpty()
    }

    @Test
    fun `should provide correctly mapped user if user exists`() = testBlocking {
        whenever(accountStore.account).thenReturn(TEST_ACCOUNT)
        reinitialize()

        val user = sut.user.first()

        softlyAssertUser(user)
    }

    @Test
    fun `should not provide user if user does not exist`() = testBlocking {
        whenever(accountStore.account).thenReturn(null)
        reinitialize()

        val user = sut.user.first()

        assertThat(user).isNull()
    }

    @Test
    fun `should not provide user if the account is the default one`() = testBlocking {
        whenever(accountStore.account).thenReturn(DEFAULT_TEST_ACCOUNT)
        reinitialize()

        val user = sut.user.first()

        assertThat(user).isNull()
    }

    @Test
    fun `should provide updated user if user changed`() = testBlocking {
        whenever(accountStore.account).thenReturn(null)
        reinitialize()

        whenever(accountStore.account).thenReturn(TEST_ACCOUNT)
        sut.onAccountChanged(AccountStore.OnAccountChanged())
        val user = sut.user.first()

        softlyAssertUser(user)
    }

    @Test
    fun `should provide recent locale after locale change`() {
        whenever(loginRepository.selectedSiteFlow).thenReturn(MutableStateFlow(null))
        whenever(providedLocale).thenReturn(Locale.US)

        assertThat(sut.locale).isEqualTo(Locale.US)

        whenever(providedLocale).thenReturn(Locale.CANADA)

        assertThat(sut.locale).isEqualTo(Locale.CANADA)
    }

    @Test
    fun `should provide empty extras for event`() {
        val extras = sut.provideExtrasForEvent(currentExtras = emptyMap(), eventLevel = EventLevel.INFO)

        assertThat(extras).isEmpty()
    }

    @Test
    fun `should provide version name for release name for not debug build`() {
        whenever(loginRepository.selectedSiteFlow).thenReturn(MutableStateFlow(null))
        whenever(BuildConfig.DEBUG).thenReturn(false)

        reinitialize()

        assertThat(sut.releaseName).isEqualTo(ReleaseName.SetByTracksLibrary)
    }

    @Test
    fun `should provide debug name for release name for debug build`() {
        whenever(loginRepository.selectedSiteFlow).thenReturn(MutableStateFlow(null))
        whenever(BuildConfig.DEBUG).thenReturn(true)

        reinitialize()

        assertThat(sut.releaseName).isEqualTo(ReleaseName.SetByApplication(WCWearCrashLoggingDataProvider.DEBUG_RELEASE_NAME))
    }

    private fun softlyAssertUser(user: CrashLoggingUser?) {
        SoftAssertions().apply {
            assertThat(user?.username).isEqualTo(TEST_ACCOUNT.userName)
            assertThat(user?.email).isEqualTo(TEST_ACCOUNT.email)
            assertThat(user?.userID).isEqualTo(TEST_ACCOUNT.userId.toString())
        }.assertAll()
    }

    companion object {
        val TEST_ACCOUNT = AccountModel().apply {
            userId = 123L
            email = "mail@a8c.com"
            userName = "username"
        }

        val DEFAULT_TEST_ACCOUNT = AccountModel()

        val TEST_SITE_MODEL = SiteModel().apply {
            siteId = 7L
            url = "automattic.com"
        }
    }
}
