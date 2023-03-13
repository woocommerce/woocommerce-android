package com.woocommerce.android.support

import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.zendesk.TicketType
import com.woocommerce.android.support.zendesk.ZendeskEnvironmentDataSource
import com.woocommerce.android.support.zendesk.ZendeskException.IdentityNotSetException
import com.woocommerce.android.support.zendesk.ZendeskException.RequestCreationFailedException
import com.woocommerce.android.support.zendesk.ZendeskException.RequestCreationTimeoutException
import com.woocommerce.android.support.zendesk.ZendeskTicketRepository
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.support.zendesk.ZendeskTags
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.zendesk.service.ErrorResponse
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
import zendesk.support.CreateRequest
import zendesk.support.Request
import zendesk.support.RequestProvider

@OptIn(ExperimentalCoroutinesApi::class)
internal class ZendeskTicketRepositoryTest : BaseUnitTest() {
    private lateinit var sut: ZendeskTicketRepository
    private lateinit var zendeskSettings: ZendeskSettings
    private lateinit var requestProvider: RequestProvider
    private lateinit var envDataSource: ZendeskEnvironmentDataSource
    private lateinit var siteStore: SiteStore

    @Before
    fun setup() {
        requestProvider = mock()
        zendeskSettings = mock {
            on { isIdentitySet } doReturn true
            on { requestProvider } doReturn requestProvider
        }
        siteStore = mock {
            on { sites } doReturn emptyList()
        }
        envDataSource = mockEnvDataSource()
        createSUT()
    }

    @Test
    fun `when createRequest is called with no identity set, then an result with IdentityNotSetException is emitted`() =
        testBlocking {
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
    fun `when createRequest is called correctly, then an result with the Request is emitted`() = testBlocking {
        // Given
        var result: Result<Request?>? = null
        val expectedRequest = Request()
        val captor = argumentCaptor<ZendeskCallback<Request>>()

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

        verify(requestProvider).createRequest(any(), captor.capture())
        captor.firstValue.onSuccess(expectedRequest)
        advanceUntilIdle()
        job.cancel()

        // Then
        assertThat(result).isNotNull
        assertThat(result?.isSuccess).isTrue
        assertThat(result?.isFailure).isFalse
        assertThat(result?.getOrNull()).isNotNull
        assertThat(result?.getOrNull()).isEqualTo(expectedRequest)
    }

    @Test
    fun `when createRequest is fails, then an result with an exception is emitted`() = testBlocking {
        // Given
        var result: Result<Request?>? = null
        val captor = argumentCaptor<ZendeskCallback<Request>>()
        val errorMessage = "Error message"
        val error = mock<ErrorResponse> {
            on { reason } doReturn errorMessage
        }

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

        // Then
        verify(requestProvider).createRequest(any(), captor.capture())
        captor.firstValue.onError(error)
        advanceUntilIdle()
        job.cancel()

        assertThat(result).isNotNull
        assertThat(result?.isSuccess).isFalse
        assertThat(result?.isFailure).isTrue
        assertThat(result?.exceptionOrNull()).isInstanceOf(RequestCreationFailedException::class.java)
        assertThat(result?.exceptionOrNull()?.message).isEqualTo(errorMessage)
    }

    @Test
    fun `when createRequest timeout, then an result with an exception is emitted`() = testBlocking {
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
        advanceUntilIdle()
        job.cancel()

        // Then
        assertThat(result).isNotNull
        assertThat(result?.isSuccess).isFalse
        assertThat(result?.isFailure).isTrue
        assertThat(result?.exceptionOrNull()).isEqualTo(RequestCreationTimeoutException)
    }

    @Test
    fun `when createRequest is called, then the request is created with the correct parameters`() = testBlocking {
        // Given
        val expectedSubject = "subject"
        val expectedDescription = "description"
        val expectedTags = arrayOf("tag1", "tag2")
        val captor = argumentCaptor<CreateRequest>()

        // When
        val job = launch {
            sut.createRequest(
                context = mock(),
                origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                ticketType = TicketType.MobileApp,
                selectedSite = null,
                subject = expectedSubject,
                description = expectedDescription,
                extraTags = expectedTags.toList()
            ).first()
        }

        // Then
        verify(requestProvider).createRequest(captor.capture(), any())
        advanceUntilIdle()
        job.cancel()

        val actualRequest = captor.firstValue
        assertThat(actualRequest.description).isEqualTo(expectedDescription)
        assertThat(actualRequest.subject).isEqualTo(expectedSubject)
        assertThat(actualRequest.tags).contains(*expectedTags)
    }

    @Test
    fun `when createRequest is called using MobileApp as ticketType, then the request is created with the expected tags`() =
        testBlocking {
            // Given
            val expectedTags = arrayOf(ZendeskTags.mobileApp)
            val captor = argumentCaptor<CreateRequest>()

            // When
            val job = launch {
                sut.createRequest(
                    context = mock(),
                    origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                    ticketType = TicketType.MobileApp,
                    selectedSite = null,
                    subject = "subject",
                    description = "description",
                    extraTags = emptyList()
                ).first()
            }

            // Then
            verify(requestProvider).createRequest(captor.capture(), any())
            advanceUntilIdle()
            job.cancel()

            val actualRequest = captor.firstValue
            assertThat(actualRequest.tags).contains(*expectedTags)
        }

    private fun createSUT() {
        sut = ZendeskTicketRepository(
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
