package com.woocommerce.android.support

import com.woocommerce.android.applicationpasswords.IsAppPasswordsSupportedForJetpackSite
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.zendesk.MobileStatusProvider
import com.woocommerce.android.support.zendesk.TicketCustomField
import com.woocommerce.android.support.zendesk.TicketType
import com.woocommerce.android.support.zendesk.ZendeskEnvironmentDataSource
import com.woocommerce.android.support.zendesk.ZendeskException.IdentityNotSetException
import com.woocommerce.android.support.zendesk.ZendeskException.RequestCreationFailedException
import com.woocommerce.android.support.zendesk.ZendeskException.RequestCreationTimeoutException
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.support.zendesk.ZendeskTags
import com.woocommerce.android.support.zendesk.ZendeskTicketRepository
import com.woocommerce.android.util.WCSSRModelCachingFetcher
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
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
import zendesk.support.CreateRequest
import zendesk.support.Request
import zendesk.support.RequestProvider
import zendesk.support.UploadProvider
import zendesk.support.UploadResponse

@OptIn(ExperimentalCoroutinesApi::class)
internal class ZendeskTicketRepositoryTest : BaseUnitTest() {
    private lateinit var sut: ZendeskTicketRepository
    private lateinit var zendeskSettings: ZendeskSettings
    private lateinit var requestProvider: RequestProvider
    private lateinit var envDataSource: ZendeskEnvironmentDataSource
    private lateinit var siteStore: SiteStore
    private val ssrFetcher: WCSSRModelCachingFetcher = mock {
        on { load(any(), any()) } doReturn WooResult(model = null)
    }
    private val isAppPasswordsSupportedForJetpackSite: IsAppPasswordsSupportedForJetpackSite = mock()
    private val mobileStatusProvider: MobileStatusProvider = mock {
        on { invoke(anyOrNull(), anyOrNull()) } doReturn MSR_REPORT
    }

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

            ssrFetcher.stub {
                on { load(selectedSite, false) } doReturn WooResult(model = WCSSRModel(123))
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
            ssrFetcher.stub {
                on { load(any(), any()) } doReturn WooResult(model = WCSSRModel(123))
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
    fun `when creating the request, then the mobile app status report is attached`() =
        testBlocking {
            // given
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
            assertThat(captor.firstValue.customFields).anySatisfy {
                assertThat(it.id).isEqualTo(TicketCustomField.msr)
                assertThat(it.valueString).isEqualTo(MSR_REPORT)
            }
        }

    @Test
    fun `given no site is selected, when creating the request, then the mobile app status report is still attached`() =
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
            assertThat(captor.firstValue.customFields).anySatisfy {
                assertThat(it.id).isEqualTo(TicketCustomField.msr)
                assertThat(it.valueString).isEqualTo(MSR_REPORT)
            }
        }

    @Test
    fun `given the ssr report is returned and site is selected, when creating the request, attach ssr`() =
        testBlocking {
            // given
            ssrFetcher.stub {
                on { load(any(), any()) } doReturn WooResult(model = WCSSRModel(123))
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
            ssrFetcher.stub {
                on { load(any(), any()) } doReturn WooResult(
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

    @Test
    fun `given jetpack site supports app passwords, when creating the request, then include corresponding tag`() =
        testBlocking {
            val site = SiteModel().apply {
                origin = SiteModel.ORIGIN_WPCOM_REST
                setIsJetpackConnected(true)
            }
            given(isAppPasswordsSupportedForJetpackSite.invoke(site)).willReturn(true)

            val captor = argumentCaptor<CreateRequest>()
            createSUT()

            // when
            sut.createRequest(
                context = mock(),
                origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                ticketType = TicketType.MobileApp,
                selectedSite = site,
                subject = "subject",
                description = "description",
                extraTags = emptyList(),
                siteAddress = "siteAddress"
            ).first()

            // then
            verify(requestProvider).createRequest(captor.capture(), any())
            val tags = captor.firstValue.tags
            assertThat(tags).contains(ZendeskTags.jetpackSiteUsingAppPasswords)
        }

    @Test
    fun `when createRequest is called, then the full device logs are uploaded as an attachment and its token is set`() =
        testBlocking {
            // given
            val expectedToken = "attachment-token"
            val uploadResponse = mock<UploadResponse> { on { token } doReturn expectedToken }
            val uploadProvider = mock<UploadProvider>()
            given(zendeskSettings.uploadProvider).willReturn(uploadProvider)
            val uploadCaptor = argumentCaptor<ZendeskCallback<UploadResponse>>()
            val requestCaptor = argumentCaptor<CreateRequest>()

            // when
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

            // then
            verify(uploadProvider).uploadAttachment(
                eq("application_log.txt"),
                any(),
                eq("text/plain"),
                uploadCaptor.capture()
            )
            uploadCaptor.firstValue.onSuccess(uploadResponse)
            advanceUntilIdle()
            verify(requestProvider).createRequest(requestCaptor.capture(), any())
            job.cancel()

            assertThat(requestCaptor.firstValue.attachments).containsExactly(expectedToken)
        }

    @Test
    fun `given the log upload fails, when createRequest is called, then the request is still created without attachments`() =
        testBlocking {
            // given
            val uploadProvider = mock<UploadProvider>()
            given(zendeskSettings.uploadProvider).willReturn(uploadProvider)
            val uploadCaptor = argumentCaptor<ZendeskCallback<UploadResponse>>()
            val requestCaptor = argumentCaptor<CreateRequest>()

            // when
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

            // then
            verify(uploadProvider).uploadAttachment(
                eq("application_log.txt"),
                any(),
                any(),
                uploadCaptor.capture()
            )
            uploadCaptor.firstValue.onError(mock())
            advanceUntilIdle()
            verify(requestProvider).createRequest(requestCaptor.capture(), any())
            job.cancel()

            assertThat(requestCaptor.firstValue.attachments).isNullOrEmpty()
        }

    @Test
    fun `given the log upload never returns, when createRequest is called, then it times out and creates the request`() =
        testBlocking {
            // given the upload provider never invokes either callback
            val uploadProvider = mock<UploadProvider>()
            given(zendeskSettings.uploadProvider).willReturn(uploadProvider)
            val requestCaptor = argumentCaptor<CreateRequest>()

            // when
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

            // then the upload times out, is treated as no attachment, and the ticket is still created
            verify(uploadProvider).uploadAttachment(eq("application_log.txt"), any(), any(), any())
            advanceUntilIdle()
            verify(requestProvider).createRequest(requestCaptor.capture(), any())
            job.cancel()

            assertThat(requestCaptor.firstValue.attachments).isNullOrEmpty()
        }

    @Test
    fun `given a diagnostic log, when createRequest is called, then both the diagnostic and full logs are attached`() =
        testBlocking {
            // given
            val diagnosticResponse = mock<UploadResponse> { on { token } doReturn "diagnostic-token" }
            val appLogResponse = mock<UploadResponse> { on { token } doReturn "app-log-token" }
            val uploadProvider = mock<UploadProvider>()
            given(zendeskSettings.uploadProvider).willReturn(uploadProvider)
            val uploadCaptor = argumentCaptor<ZendeskCallback<UploadResponse>>()
            val requestCaptor = argumentCaptor<CreateRequest>()

            // when
            val job = launch {
                sut.createRequest(
                    context = mock(),
                    origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                    ticketType = TicketType.MobileApp,
                    selectedSite = null,
                    subject = "subject",
                    description = "description",
                    extraTags = emptyList(),
                    siteAddress = "siteAddress",
                    diagnosticLog = "diagnostic logs"
                ).first()
            }

            // then
            verify(uploadProvider).uploadAttachment(
                eq("connectivitytest_log.txt"),
                any(),
                any(),
                uploadCaptor.capture()
            )
            uploadCaptor.firstValue.onSuccess(diagnosticResponse)
            verify(uploadProvider).uploadAttachment(
                eq("application_log.txt"),
                any(),
                any(),
                uploadCaptor.capture()
            )
            uploadCaptor.secondValue.onSuccess(appLogResponse)
            advanceUntilIdle()
            verify(requestProvider).createRequest(requestCaptor.capture(), any())
            job.cancel()

            assertThat(requestCaptor.firstValue.attachments).containsExactly("diagnostic-token", "app-log-token")
        }

    @Test
    fun `when createRequest is called, then the mobile app status report is attached as a file`() =
        testBlocking {
            // given
            val appLogResponse = mock<UploadResponse> { on { token } doReturn "app-log-token" }
            val statusResponse = mock<UploadResponse> { on { token } doReturn "status-token" }
            val uploadProvider = mock<UploadProvider>()
            given(zendeskSettings.uploadProvider).willReturn(uploadProvider)
            val uploadCaptor = argumentCaptor<ZendeskCallback<UploadResponse>>()
            val requestCaptor = argumentCaptor<CreateRequest>()

            // when
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

            // then
            verify(uploadProvider).uploadAttachment(
                eq("application_log.txt"),
                any(),
                any(),
                uploadCaptor.capture()
            )
            uploadCaptor.firstValue.onSuccess(appLogResponse)
            verify(uploadProvider).uploadAttachment(
                eq("mobile_status_report.txt"),
                any(),
                any(),
                uploadCaptor.capture()
            )
            uploadCaptor.secondValue.onSuccess(statusResponse)
            advanceUntilIdle()
            verify(requestProvider).createRequest(requestCaptor.capture(), any())
            job.cancel()

            assertThat(requestCaptor.firstValue.attachments).containsExactly("app-log-token", "status-token")
        }

    @Test
    fun `when createRequest is called, then all the attachments are uploaded before any of them completes`() =
        testBlocking {
            // given
            val uploadProvider = mock<UploadProvider>()
            given(zendeskSettings.uploadProvider).willReturn(uploadProvider)

            // when
            val job = launch {
                sut.createRequest(
                    context = mock(),
                    origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                    ticketType = TicketType.MobileApp,
                    selectedSite = null,
                    subject = "subject",
                    description = "description",
                    extraTags = emptyList(),
                    siteAddress = "siteAddress",
                    diagnosticLog = "diagnostic logs"
                ).first()
            }

            // then all three uploads are in flight even though none of them has invoked its callback yet
            verify(uploadProvider).uploadAttachment(eq("connectivitytest_log.txt"), any(), any(), any())
            verify(uploadProvider).uploadAttachment(eq("application_log.txt"), any(), any(), any())
            verify(uploadProvider).uploadAttachment(eq("mobile_status_report.txt"), any(), any(), any())
            advanceUntilIdle()
            job.cancel()
        }

    @Test
    fun `given one attachment upload fails, when createRequest is called, then the others are still attached`() =
        testBlocking {
            // given
            val diagnosticResponse = mock<UploadResponse> { on { token } doReturn "diagnostic-token" }
            val statusResponse = mock<UploadResponse> { on { token } doReturn "status-token" }
            val uploadProvider = mock<UploadProvider>()
            given(zendeskSettings.uploadProvider).willReturn(uploadProvider)
            val diagnosticCaptor = argumentCaptor<ZendeskCallback<UploadResponse>>()
            val appLogCaptor = argumentCaptor<ZendeskCallback<UploadResponse>>()
            val statusCaptor = argumentCaptor<ZendeskCallback<UploadResponse>>()
            val requestCaptor = argumentCaptor<CreateRequest>()

            // when
            val job = launch {
                sut.createRequest(
                    context = mock(),
                    origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
                    ticketType = TicketType.MobileApp,
                    selectedSite = null,
                    subject = "subject",
                    description = "description",
                    extraTags = emptyList(),
                    siteAddress = "siteAddress",
                    diagnosticLog = "diagnostic logs"
                ).first()
            }

            verify(uploadProvider).uploadAttachment(
                eq("connectivitytest_log.txt"),
                any(),
                any(),
                diagnosticCaptor.capture()
            )
            verify(uploadProvider).uploadAttachment(
                eq("application_log.txt"),
                any(),
                any(),
                appLogCaptor.capture()
            )
            verify(uploadProvider).uploadAttachment(
                eq("mobile_status_report.txt"),
                any(),
                any(),
                statusCaptor.capture()
            )
            appLogCaptor.firstValue.onError(mock())
            diagnosticCaptor.firstValue.onSuccess(diagnosticResponse)
            statusCaptor.firstValue.onSuccess(statusResponse)
            advanceUntilIdle()

            // then
            verify(requestProvider).createRequest(requestCaptor.capture(), any())
            job.cancel()

            assertThat(requestCaptor.firstValue.attachments).containsExactly("diagnostic-token", "status-token")
        }

    private fun createSUT() {
        sut = ZendeskTicketRepository(
            zendeskSettings = zendeskSettings,
            envDataSource = envDataSource,
            siteStore = siteStore,
            dispatchers = coroutinesTestRule.testDispatchers,
            mock(),
            ssrFetcher,
            isAppPasswordsSupportedForJetpackSite = isAppPasswordsSupportedForJetpackSite,
            mobileStatusProvider = mobileStatusProvider
        )
    }

    private fun mockEnvDataSource() = mock<ZendeskEnvironmentDataSource> {
        on { totalAvailableMemorySize } doReturn "100"
        on { deviceLanguage } doReturn "testLanguage"
        on { getFullDeviceLogs() } doReturn "full logs"
        on { trimDeviceLogs(any()) } doReturn "logs"
        on { generateVersionName(any()) } doReturn "version"
        on { generateNetworkInformation(any()) } doReturn "networkInfo"
        on { generateCombinedLogInformationOfSites(any()) } doReturn "sitesInfo"
    }

    private companion object {
        const val MSR_REPORT = "mobile app status report"
    }
}
