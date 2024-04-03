package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.CrashLoggingUser
import com.automattic.android.tracks.crashlogging.EventLevel.FATAL
import com.automattic.android.tracks.crashlogging.EventLevel.INFO
import com.automattic.android.tracks.crashlogging.PerformanceMonitoringConfig
import com.automattic.android.tracks.crashlogging.ReleaseName
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.crashlogging.WCCrashLoggingDataProvider.Companion.DEBUG_RELEASE_NAME
import com.woocommerce.android.util.crashlogging.WCCrashLoggingDataProvider.Companion.EXTRA_UUID
import com.woocommerce.android.util.crashlogging.WCCrashLoggingDataProvider.Companion.SITE_ID_KEY
import com.woocommerce.android.util.crashlogging.WCCrashLoggingDataProvider.Companion.SITE_URL_KEY
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class WCCrashLoggingDataProviderTest : BaseUnitTest() {
    lateinit var sut: WCCrashLoggingDataProvider

    private val localeProvider: LocaleProvider = mock()
    private val accountStore: AccountStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val appPrefs: AppPrefs = mock()
    private val enqueueSendingEncryptedLogs: EnqueueSendingEncryptedLogs = mock()
    private val uuidGenerator: UuidGenerator = mock()
    private val buildConfig: BuildConfigWrapper = mock()
    private val specifyPerformanceMonitoringConfig: SpecifyPerformanceMonitoringConfig = mock {
        on { invoke() } doReturn PerformanceMonitoringConfig.Enabled(1.0)
    }

    @Before
    fun setUp() {
        sut = WCCrashLoggingDataProvider(
            localeProvider = localeProvider,
            accountStore = accountStore,
            selectedSite = selectedSite,
            appPrefs = appPrefs,
            enqueueSendingEncryptedLogs = enqueueSendingEncryptedLogs,
            uuidGenerator = uuidGenerator,
            buildConfig = buildConfig,
            appScope = TestScope(coroutinesTestRule.testDispatcher),
            dispatcher = Dispatcher(),
            specifyPerformanceMonitoringConfig = specifyPerformanceMonitoringConfig
        )
    }

    private fun reinitialize() {
        setUp()
    }

    @Test
    fun `should provide site id and site url in apps context if selected site exists`() = testBlocking {
        whenever(selectedSite.observe()).thenReturn(flowOf(TEST_SITE_MODEL))
        reinitialize()

        val appContext = sut.applicationContextProvider.single()

        assertThat(appContext).containsAllEntriesOf(
            mapOf(
                SITE_ID_KEY to TEST_SITE_MODEL.siteId.toString(),
                SITE_URL_KEY to TEST_SITE_MODEL.url
            )
        )
    }

    @Test
    fun `should not send site id if it has the default value`() = testBlocking {
        whenever(selectedSite.observe()).thenReturn(flowOf(SiteModel()))
        reinitialize()

        val appContext = sut.applicationContextProvider.single()

        assertThat(appContext).doesNotContainKey(SITE_ID_KEY)
    }

    @Test
    fun `should provide empty apps context if selected site does not exist`() = testBlocking {
        whenever(selectedSite.observe()).thenReturn(flowOf(null))
        reinitialize()

        val appContext = sut.applicationContextProvider.first()

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
        assertThat(sut.extraKnownKeys()).containsOnly(EXTRA_UUID)
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
        whenever(localeProvider.provideLocale()).thenReturn(Locale.US)

        assertThat(sut.locale).isEqualTo(Locale.US)

        whenever(localeProvider.provideLocale()).thenReturn(Locale.CANADA)

        assertThat(sut.locale).isEqualTo(Locale.CANADA)
    }

    @Test
    fun `should request encrypted logs upload when providing extras for event`() {
        val generatedUuid = "123"
        whenever(uuidGenerator.generateUuid()).thenReturn(generatedUuid)

        val extras = sut.provideExtrasForEvent(currentExtras = emptyMap(), eventLevel = INFO)

        verify(enqueueSendingEncryptedLogs, times(1)).invoke(generatedUuid, INFO)
        assertThat(extras).containsValue(generatedUuid)
    }

    @Test
    fun `should not request encrypted logs upload when uuid is already provided`() {
        val generatedUuid = "123"

        val extras = sut.provideExtrasForEvent(currentExtras = mapOf("uuid" to generatedUuid), eventLevel = INFO)

        verify(enqueueSendingEncryptedLogs, never()).invoke(any(), any())
        assertThat(extras).containsValue(generatedUuid)
    }

    @Test
    fun `should not upload immediately when event is fatal`() {
        val generatedUuid = "123"
        whenever(uuidGenerator.generateUuid()).thenReturn(generatedUuid)

        val extras = sut.provideExtrasForEvent(currentExtras = emptyMap(), eventLevel = FATAL)

        verify(enqueueSendingEncryptedLogs, times(1)).invoke(generatedUuid, FATAL)
        assertThat(extras).containsValue(generatedUuid)
    }

    @Test
    fun `should provide version name for release name for not debug build`() {
        whenever(buildConfig.debug).thenReturn(false)

        reinitialize()

        assertThat(sut.releaseName).isEqualTo(ReleaseName.SetByTracksLibrary)
    }

    @Test
    fun `should provide debug name for release name for debug build`() {
        whenever(buildConfig.debug).thenReturn(true)

        reinitialize()

        assertThat(sut.releaseName).isEqualTo(ReleaseName.SetByApplication(DEBUG_RELEASE_NAME))
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
