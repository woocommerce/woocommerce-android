package com.woocommerce.android.support

import com.woocommerce.android.support.zendesk.ZendeskException.IdentityNotSetException
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.zendesk.TicketType
import com.woocommerce.android.support.zendesk.ZendeskEnvironmentDataSource
import com.woocommerce.android.support.zendesk.ZendeskManager
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.zendesk.service.ZendeskCallback
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.store.SiteStore
import zendesk.support.Request

@OptIn(ExperimentalCoroutinesApi::class)
internal class ZendeskManagerTest : BaseUnitTest() {
    private lateinit var sut: ZendeskManager
    private lateinit var zendeskSettings: ZendeskSettings
    private lateinit var envDataSource: ZendeskEnvironmentDataSource
    private lateinit var siteStore: SiteStore
    private val captor = argumentCaptor<ZendeskCallback<Request>>()

    @Before
    fun setup() {
        zendeskSettings = mock {
            on { isIdentitySet } doReturn true
        }
        siteStore = mock {
            on { sites } doReturn emptyList()
        }
        envDataSource = mockEnvDataSource()
        createSUT()
    }

    @Test
    fun `when createRequest is called with no identity set, then an result with IdentityNotSetException is emitted`()
    = testBlocking {
        // Given
        zendeskSettings = mock { on { isIdentitySet } doReturn false }
        createSUT()

        // When
        val result = sut.createRequest(
            context = mock(),
            origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
            ticketType = TicketType.MobileApp,
            selectedSite = mock(),
            subject = "subject",
            description = "description",
            extraTags = emptyList()
        ).single()

        // Then
        assertThat(result).isNotNull
        assertThat(result.isFailure).isTrue
        assertThat(result.exceptionOrNull()).isEqualTo(IdentityNotSetException)
    }

    @Test
    fun `when createRequest is called correctly, then an result with the Request is emitted` () = testBlocking {
        // Given
        var result: Result<Request?>? = null


        // When
        val job = launch {
             result = sut.createRequest(
                context = mock(),
                origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                ticketType = TicketType.MobileApp,
                selectedSite = null,
                subject = "subject",
                description = "description",
                extraTags = emptyList()
            ).first()
        }

        verify(zendeskSettings).requestProvider?.createRequest(any(), captor.capture())
        captor.firstValue.onSuccess(Request())
        advanceUntilIdle()
        job.cancel()

        // Then
        assertThat(result).isNotNull
        assertThat(result?.isSuccess).isTrue
    }

    private fun createSUT() {
        sut = ZendeskManager(
            zendeskSettings = zendeskSettings,
            envDataSource = envDataSource,
            siteStore = siteStore,
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    private fun mockEnvDataSource() = mock<ZendeskEnvironmentDataSource> {
        on { totalAvailableMemorySize } doReturn "100"
        on { deviceLanguage } doReturn "testLanguage"
        on { deviceLogs } doReturn "logs"
        on { generateVersionName(any()) } doReturn "version"
        on { generateNetworkInformation(any()) } doReturn "networkInfo"
        on { generateCombinedLogInformationOfSites(any()) } doReturn "sitesInfo"
        on { generateHostData(any()) } doReturn "hostInfo"
    }
}
