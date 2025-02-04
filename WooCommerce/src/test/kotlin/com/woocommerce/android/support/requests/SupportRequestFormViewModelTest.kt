package com.woocommerce.android.support.requests

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.RequestCreationFailed
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.RequestCreationSucceeded
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.ShowSupportIdentityInputDialog
import com.woocommerce.android.support.zendesk.TicketType
import com.woocommerce.android.support.zendesk.ZendeskException.IdentityNotSetException
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.support.zendesk.ZendeskTicketRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel
import zendesk.support.Request

@ExperimentalCoroutinesApi
internal class SupportRequestFormViewModelTest : BaseUnitTest() {
    private lateinit var sut: SupportRequestFormViewModel
    private lateinit var zendeskTicketRepository: ZendeskTicketRepository
    private lateinit var zendeskSettings: ZendeskSettings
    private lateinit var selectedSite: SelectedSite
    private lateinit var tracks: AnalyticsTrackerWrapper
    private val savedState = SavedStateHandle()

    @Before
    fun setUp() {
        configureMocks(requestResult = Result.success(Request()))
    }

    @Test
    fun `when all fields are filled, then submit button is enabled`() = testBlocking {
        // Given
        val isSubmitButtonEnabled = mutableListOf<Boolean>()
        sut.isSubmitButtonEnabled.observeForever {
            isSubmitButtonEnabled.add(it)
        }

        // When
        sut.onSubjectChanged("subject")
        sut.onMessageChanged("message")
        sut.onSiteAddressChanged("siteAddress")
        sut.onHelpOptionSelected(TicketType.MobileApp)

        // Then
        assertThat(isSubmitButtonEnabled).hasSize(2)
        assertThat(isSubmitButtonEnabled[0]).isFalse
        assertThat(isSubmitButtonEnabled[1]).isTrue
    }

    @Test
    fun `when text fields are empty, then submit button is disabled`() = testBlocking {
        // Given
        val isSubmitButtonEnabled = mutableListOf<Boolean>()
        sut.isSubmitButtonEnabled.observeForever {
            isSubmitButtonEnabled.add(it)
        }

        // When
        sut.onSubjectChanged("")
        sut.onMessageChanged("")
        sut.onSiteAddressChanged("")
        sut.onHelpOptionSelected(TicketType.MobileApp)

        // Then
        assertThat(isSubmitButtonEnabled).hasSize(1)
        assertThat(isSubmitButtonEnabled[0]).isFalse
    }

    @Test
    fun `when view is loading, then submit button is disabled`() = testBlocking {
        // Given
        val isSubmitButtonEnabled = mutableListOf<Boolean>()
        sut.isSubmitButtonEnabled.observeForever {
            isSubmitButtonEnabled.add(it)
        }

        // When
        sut.onSubjectChanged("subject")
        sut.onMessageChanged("message")
        sut.onSiteAddressChanged("site Address")
        sut.onHelpOptionSelected(TicketType.MobileApp)
        sut.submitSupportRequest(mock(), HelpOrigin.LOGIN_HELP_NOTIFICATION, emptyList())

        // Then
        assertThat(isSubmitButtonEnabled).hasSize(4)
        assertThat(isSubmitButtonEnabled[0]).isFalse
        assertThat(isSubmitButtonEnabled[1]).isTrue
        assertThat(isSubmitButtonEnabled[2]).isFalse
        assertThat(isSubmitButtonEnabled[3]).isTrue
    }

    @Test
    fun `when view is loading, then request is triggered and isRequestLoading is updated`() = testBlocking {
        // Given
        val isRequestLoading = mutableListOf<Boolean>()
        sut.isRequestLoading.observeForever {
            isRequestLoading.add(it)
        }

        // When
        sut.onHelpOptionSelected(TicketType.MobileApp)
        sut.submitSupportRequest(mock(), HelpOrigin.LOGIN_HELP_NOTIFICATION, emptyList())

        // Then
        assertThat(isRequestLoading).hasSize(3)
        assertThat(isRequestLoading[0]).isFalse
        assertThat(isRequestLoading[1]).isTrue
        assertThat(isRequestLoading[2]).isFalse
        verify(zendeskTicketRepository, times(1)).createRequest(any(), any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `when submit request is triggered without a help option selected, then ignore the request`() = testBlocking {
        // Given
        val isRequestLoading = mutableListOf<Boolean>()
        sut.isRequestLoading.observeForever {
            isRequestLoading.add(it)
        }

        // When
        sut.submitSupportRequest(mock(), HelpOrigin.LOGIN_HELP_NOTIFICATION, emptyList())

        // Then
        assertThat(isRequestLoading).hasSize(1)
        assertThat(isRequestLoading[0]).isFalse
        verify(zendeskTicketRepository, never()).createRequest(any(), any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `when submit request succeed, then trigger the expected event`() = testBlocking {
        // Given
        val event = mutableListOf<MultiLiveEvent.Event>()
        sut.event.observeForever {
            event.add(it)
        }

        // When
        sut.onHelpOptionSelected(TicketType.MobileApp)
        sut.submitSupportRequest(mock(), HelpOrigin.LOGIN_HELP_NOTIFICATION, emptyList())

        // Then
        assertThat(event).hasSize(1)
        assertThat(event[0]).isEqualTo(RequestCreationSucceeded)
        verify(tracks, times(1)).track(AnalyticsEvent.SUPPORT_NEW_REQUEST_CREATED)
    }

    @Test
    fun `when submit request fails, then trigger the expected event`() = testBlocking {
        // Given
        configureMocks(requestResult = Result.failure(Exception()))
        val event = mutableListOf<MultiLiveEvent.Event>()
        sut.event.observeForever {
            event.add(it)
        }

        // When
        sut.onHelpOptionSelected(TicketType.MobileApp)
        sut.submitSupportRequest(mock(), HelpOrigin.LOGIN_HELP_NOTIFICATION, emptyList())

        // Then
        assertThat(event).hasSize(1)
        assertThat(event[0]).isEqualTo(RequestCreationFailed)
        verify(tracks, times(1)).track(AnalyticsEvent.SUPPORT_NEW_REQUEST_FAILED)
    }

    @Test
    fun `when submit request fails with identity error, then trigger the expected event`() = testBlocking {
        // Given
        configureMocks(requestResult = Result.failure(IdentityNotSetException))
        val event = mutableListOf<MultiLiveEvent.Event>()
        sut.event.observeForever {
            event.add(it)
        }

        // When
        sut.onHelpOptionSelected(TicketType.MobileApp)
        sut.submitSupportRequest(mock(), HelpOrigin.LOGIN_HELP_NOTIFICATION, emptyList())

        // Then
        assertThat(event).hasSize(1)
        assertThat(event[0]).isInstanceOf(ShowSupportIdentityInputDialog::class.java)
        verify(tracks, times(1)).track(AnalyticsEvent.SUPPORT_NEW_REQUEST_FAILED)
    }

    @Test
    fun `when onViewCreated is called, then trigger the expected track event`() {
        // When
        sut.onViewCreated()

        // Then
        verify(tracks, times(1)).track(AnalyticsEvent.SUPPORT_NEW_REQUEST_VIEWED)
    }

    @Test
    fun `when onUserIdentitySet is called, then run the expected actions`() = testBlocking {
        // Given
        val email = "email@test.com"
        val name = "test name"

        // When
        sut.onHelpOptionSelected(TicketType.MobileApp)
        sut.onUserIdentitySet(mock(), HelpOrigin.LOGIN_HELP_NOTIFICATION, emptyList(), email, name)

        // Then
        verify(zendeskSettings).supportEmail = email
        verify(zendeskSettings).supportName = name
        verify(tracks, times(1)).track(AnalyticsEvent.SUPPORT_IDENTITY_SET)
        verify(zendeskTicketRepository, times(1)).createRequest(any(), any(), any(), any(), any(), any(), any(), any())
    }

    private fun configureMocks(requestResult: Result<Request?>) {
        zendeskSettings = mock()
        tracks = mock()
        val testSite = SiteModel().apply { id = 123 }
        selectedSite = mock {
            on { getIfExists() }.then { testSite }
        }
        zendeskTicketRepository = mock {
            onBlocking {
                createRequest(
                    any(),
                    any(),
                    any(),
                    eq(testSite),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } doReturn flowOf(requestResult)
        }

        sut = SupportRequestFormViewModel(
            zendeskTicketRepository = zendeskTicketRepository,
            zendeskSettings = zendeskSettings,
            selectedSite = selectedSite,
            tracks = tracks,
            savedState = savedState
        )
    }
}
