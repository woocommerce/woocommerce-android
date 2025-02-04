package com.woocommerce.android.support

import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.zendesk.TicketCustomField
import com.woocommerce.android.support.zendesk.TicketType
import com.woocommerce.android.support.zendesk.ZendeskEnvironmentDataSource
import com.woocommerce.android.support.zendesk.ZendeskException.IdentityNotSetException
import com.woocommerce.android.support.zendesk.ZendeskException.RequestCreationFailedException
import com.woocommerce.android.support.zendesk.ZendeskException.RequestCreationTimeoutException
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.support.zendesk.ZendeskTags
import com.woocommerce.android.support.zendesk.ZendeskTicketRepository
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
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore
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
    private val wooStore: WooCommerceStore = mock()

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
                extraTags = emptyList(),
                siteAddress = "siteAddress"
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
                extraTags = emptyList(),
                siteAddress = "siteAddress"
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
                extraTags = emptyList(),
                siteAddress = "siteAddress"
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
                extraTags = emptyList(),
                siteAddress = "siteAddress"
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
                extraTags = expectedTags.toList(),
                siteAddress = "siteAddress"
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
            val expectedTags = arrayOf("mobile_app")
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
                    extraTags = emptyList(),
                    siteAddress = "siteAddress"
                ).first()
            }

            // Then
            verify(requestProvider).createRequest(captor.capture(), any())
            advanceUntilIdle()
            job.cancel()

            val actualRequest = captor.firstValue
            assertThat(actualRequest.tags).contains(*expectedTags)
        }

    @Test
    fun `when createRequest is called using InPersonPayments as ticketType, then the request is created with the expected tags`() =
        testBlocking {
            // Given
            val expectedTags = arrayOf(
                "woocommerce_mobile_apps",
                "product_area_apps_in_person_payments"
            )
            val captor = argumentCaptor<CreateRequest>()

            // When
            val job = launch {
                sut.createRequest(
                    context = mock(),
                    origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                    ticketType = TicketType.InPersonPayments,
                    selectedSite = null,
                    subject = "subject",
                    description = "description",
                    extraTags = emptyList(),
                    siteAddress = "siteAddress"
                ).first()
            }

            // Then
            verify(requestProvider).createRequest(captor.capture(), any())
            advanceUntilIdle()
            job.cancel()

            val actualRequest = captor.firstValue
            assertThat(actualRequest.tags).contains(*expectedTags)
        }

    @Test
    fun `when createRequest is called using Payments as ticketType, then the request is created with the expected tags`() =
        testBlocking {
            // Given
            val expectedTags = arrayOf(
                "woocommerce_payments",
                "product_area_woo_payment_gateway",
                "mobile_app_woo_transfer",
                "support",
                "payment"
            )
            val excludedTags = arrayOf("jetpack")
            val captor = argumentCaptor<CreateRequest>()

            // When
            val job = launch {
                sut.createRequest(
                    context = mock(),
                    origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                    ticketType = TicketType.Payments,
                    selectedSite = null,
                    subject = "subject",
                    description = "description",
                    extraTags = listOf(ZendeskTags.jetpackTag),
                    siteAddress = "siteAddress"
                ).first()
            }

            // Then
            verify(requestProvider).createRequest(captor.capture(), any())
            advanceUntilIdle()
            job.cancel()

            val actualRequest = captor.firstValue
            assertThat(actualRequest.tags).contains(*expectedTags)
            assertThat(actualRequest.tags).doesNotContain(*excludedTags)
        }

    @Test
    fun `when createRequest is called using WooPlugin as ticketType, then the request is created with the expected tags`() =
        testBlocking {
            // Given
            val expectedTags = arrayOf(
                "woocommerce_core",
                "mobile_app_woo_transfer",
                "support"
            )
            val excludedTags = arrayOf("jetpack")
            val captor = argumentCaptor<CreateRequest>()

            // When
            val job = launch {
                sut.createRequest(
                    context = mock(),
                    origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                    ticketType = TicketType.WooPlugin,
                    selectedSite = null,
                    subject = "subject",
                    description = "description",
                    extraTags = listOf(ZendeskTags.jetpackTag),
                    siteAddress = "siteAddress"
                ).first()
            }

            // Then
            verify(requestProvider).createRequest(captor.capture(), any())
            advanceUntilIdle()
            job.cancel()

            val actualRequest = captor.firstValue
            assertThat(actualRequest.tags).contains(*expectedTags)
            assertThat(actualRequest.tags).doesNotContain(*excludedTags)
        }

    @Test
    fun `when createRequest is called using OtherPlugins as ticketType, then the request is created with the expected tags`() =
        testBlocking {
            // Given
            val expectedTags = arrayOf(
                "product_area_woo_extensions",
                "mobile_app_woo_transfer",
                "support",
                "store"
            )
            val excludedTags = arrayOf("jetpack")
            val captor = argumentCaptor<CreateRequest>()

            // When
            val job = launch {
                sut.createRequest(
                    context = mock(),
                    origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                    ticketType = TicketType.OtherPlugins,
                    selectedSite = null,
                    subject = "subject",
                    description = "description",
                    extraTags = listOf(ZendeskTags.jetpackTag),
                    siteAddress = "siteAddress"
                ).first()
            }

            // Then
            verify(requestProvider).createRequest(captor.capture(), any())
            advanceUntilIdle()
            job.cancel()

            val actualRequest = captor.firstValue
            assertThat(actualRequest.tags).contains(*expectedTags)
            assertThat(actualRequest.tags).doesNotContain(*excludedTags)
        }

    @Test
    fun `when createRequest is called with authenticated site, then the request is created with the expected tags`() =
        testBlocking {
            // Given
            val selectedSite = mock<SiteModel> {
                on { origin } doReturn SiteModel.ORIGIN_WPAPI
            }
            val expectedTags = arrayOf("application_password_authenticated")
            val captor = argumentCaptor<CreateRequest>()
            wooStore.stub {
                onBlocking { fetchSSR(selectedSite) } doReturn WooResult(model = WCSSRModel(123))
            }

            // When
            val job = launch {
                sut.createRequest(
                    context = mock(),
                    origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                    ticketType = TicketType.MobileApp,
                    selectedSite = selectedSite,
                    subject = "subject",
                    description = "description",
                    extraTags = emptyList(),
                    siteAddress = "siteAddress"
                ).first()
            }

            // Then
            verify(requestProvider).createRequest(captor.capture(), any())
            advanceUntilIdle()
            job.cancel()

            val actualRequest = captor.firstValue
            assertThat(actualRequest.tags).contains(*expectedTags)
        }

    @Test
    fun `when createRequest is called with a WPCOM site, then the request is created with the expected tags`() =
        testBlocking {
            // Given
            val site = mock<SiteModel> { on { isWPCom } doReturn true }
            siteStore = mock { on { sites } doReturn listOf(site) }
            createSUT()
            val expectedTags = arrayOf("wpcom")
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
                    extraTags = emptyList(),
                    siteAddress = "siteAddress"
                ).first()
            }

            // Then
            verify(requestProvider).createRequest(captor.capture(), any())
            advanceUntilIdle()
            job.cancel()

            val actualRequest = captor.firstValue
            assertThat(actualRequest.tags).contains(*expectedTags)
        }

    @Test
    fun `when createRequest is called with a Jetpack connected site, then the request is created with the expected tags`() =
        testBlocking {
            // Given
            val site = mock<SiteModel> { on { isJetpackConnected } doReturn true }
            siteStore = mock { on { sites } doReturn listOf(site) }
            createSUT()
            val expectedTags = arrayOf("jetpack")
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
                    extraTags = emptyList(),
                    siteAddress = "siteAddress"
                ).first()
            }

            // Then
            verify(requestProvider).createRequest(captor.capture(), any())
            advanceUntilIdle()
            job.cancel()

            val actualRequest = captor.firstValue
            assertThat(actualRequest.tags).contains(*expectedTags)
        }

    @Test
    fun `when createRequest is called with two sites, then the request is created with both planShortName as tags`() =
        testBlocking {
            // Given
            val firstSite = mock<SiteModel> { on { planShortName } doReturn "First site plan" }
            val secondSite = mock<SiteModel> { on { planShortName } doReturn "Second site plan" }
            val thirdSite = mock<SiteModel> { on { planShortName } doReturn null }
            val fourthSite = mock<SiteModel> { on { planShortName } doReturn "Second site plan" }
            siteStore = mock { on { sites } doReturn listOf(firstSite, secondSite, thirdSite, fourthSite) }
            createSUT()
            val expectedTags = arrayOf("First site plan", "Second site plan")
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
                    extraTags = emptyList(),
                    siteAddress = "siteAddress"
                ).first()
            }

            // Then
            verify(requestProvider).createRequest(captor.capture(), any())
            advanceUntilIdle()
            job.cancel()

            val actualRequest = captor.firstValue
            assertThat(actualRequest.tags).contains(*expectedTags)
        }

    @Test
    fun `when createRequest is called, then the request is created with the origin and platform tags`() =
        testBlocking {
            // Given
            val helpOrigin = HelpOrigin.LOGIN_HELP_NOTIFICATION
            val expectedTags = arrayOf(helpOrigin.toString(), "Android")
            val captor = argumentCaptor<CreateRequest>()

            // When
            val job = launch {
                sut.createRequest(
                    context = mock(),
                    origin = helpOrigin,
                    ticketType = TicketType.MobileApp,
                    selectedSite = null,
                    subject = "subject",
                    description = "description",
                    extraTags = emptyList(),
                    siteAddress = "siteAddress"
                ).first()
            }

            // Then
            verify(requestProvider).createRequest(captor.capture(), any())
            advanceUntilIdle()
            job.cancel()

            val actualRequest = captor.firstValue
            assertThat(actualRequest.tags).contains(*expectedTags)
        }

    @Test
    fun `when createRequest is called, then the request is created with the site address`() =
        testBlocking {
            // given
            wooStore.stub {
                onBlocking { fetchSSR(any()) } doReturn WooResult(model = WCSSRModel(123))
            }
            val siteAddress = "www.test.com"
            val captor = argumentCaptor<CreateRequest>()
            createSUT()

            // when
            sut.createRequest(
                context = mock(),
                origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                ticketType = TicketType.MobileApp,
                selectedSite = SiteModel(),
                subject = "subject",
                description = "description",
                extraTags = emptyList(),
                siteAddress = siteAddress
            ).first()

            // then
            verify(requestProvider).createRequest(captor.capture(), any())

            val customFields = captor.firstValue.customFields
            assertThat(customFields).anySatisfy {
                assertThat(it.id).isEqualTo(TicketCustomField.siteAddress)
                assertThat(it.valueString).isEqualTo(siteAddress)
            }
        }

    @Test
    fun `given the ssr report is returned and site is selected, when creating the request, attach ssr`() =
        testBlocking {
            // given
            wooStore.stub {
                onBlocking { fetchSSR(any()) } doReturn WooResult(model = WCSSRModel(123))
            }
            val captor = argumentCaptor<CreateRequest>()
            createSUT()

            // when
            sut.createRequest(
                context = mock(),
                origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                ticketType = TicketType.MobileApp,
                selectedSite = SiteModel(),
                subject = "subject",
                description = "description",
                extraTags = emptyList(),
                siteAddress = "siteAddress"
            ).first()

            // then
            verify(requestProvider).createRequest(captor.capture(), any())
            val customFields = captor.firstValue.customFields
            assertThat(customFields).anySatisfy {
                assertThat(it.id).isEqualTo(TicketCustomField.ssr)
                assertThat(it.valueString).isNotBlank()
            }
        }

    @Test
    fun `given the site is not selected, when creating the request, attach empty ssr`() =
        testBlocking {
            // given
            val captor = argumentCaptor<CreateRequest>()
            createSUT()

            // when
            sut.createRequest(
                context = mock(),
                origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                ticketType = TicketType.MobileApp,
                selectedSite = null,
                subject = "subject",
                description = "description",
                extraTags = emptyList(),
                siteAddress = "siteAddress"
            ).first()

            // then
            verify(requestProvider).createRequest(captor.capture(), any())
            val customFields = captor.firstValue.customFields
            assertThat(customFields).anySatisfy {
                assertThat(it.id).isEqualTo(TicketCustomField.ssr)
                assertThat(it.valueString).isNull()
            }
        }

    @Test
    fun `given the site is selected but app fails on fetching ssr, when creating the request, attach empty ssr`() =
        testBlocking {
            // given
            wooStore.stub {
                onBlocking { fetchSSR(any()) } doReturn WooResult(
                    WooError(
                        WooErrorType.GENERIC_ERROR,
                        BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            }
            val captor = argumentCaptor<CreateRequest>()
            createSUT()

            // when
            sut.createRequest(
                context = mock(),
                origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                ticketType = TicketType.MobileApp,
                selectedSite = SiteModel(),
                subject = "subject",
                description = "description",
                extraTags = emptyList(),
                siteAddress = "siteAddress"
            ).first()

            // then
            verify(requestProvider).createRequest(captor.capture(), any())
            val customFields = captor.firstValue.customFields
            assertThat(customFields).anySatisfy {
                assertThat(it.id).isEqualTo(TicketCustomField.ssr)
                assertThat(it.valueString).isNull()
            }
        }

    private fun createSUT() {
        sut = ZendeskTicketRepository(
            zendeskSettings = zendeskSettings,
            envDataSource = envDataSource,
            siteStore = siteStore,
            dispatchers = coroutinesTestRule.testDispatchers,
            mock(),
            wooStore,
        )
    }

    private fun mockEnvDataSource() = mock<ZendeskEnvironmentDataSource> {
        on { totalAvailableMemorySize } doReturn "100"
        on { deviceLanguage } doReturn "testLanguage"
        on { deviceLogs } doReturn "logs"
        on { generateVersionName(any()) } doReturn "version"
        on { generateNetworkInformation(any()) } doReturn "networkInfo"
        on { generateCombinedLogInformationOfSites(any()) } doReturn "sitesInfo"
    }
}
