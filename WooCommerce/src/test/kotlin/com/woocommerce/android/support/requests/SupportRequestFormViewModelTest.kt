package com.woocommerce.android.support.requests

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.support.HelpOption
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel
import zendesk.support.Request

@ExperimentalCoroutinesApi
internal class SupportRequestFormViewModelTest : BaseUnitTest() {
    private lateinit var sut: SupportRequestFormViewModel
    private lateinit var zendeskHelper: ZendeskHelper
    private lateinit var selectedSite: SelectedSite
    private val savedState = SavedStateHandle()

    @Before
    fun setUp() {
        val testSite = SiteModel().apply { id = 123 }
        selectedSite = mock {
            on { get() }.then { testSite }
        }
        zendeskHelper = mock {
            onBlocking { createRequest(any(), any(), eq(testSite), any(), any(), any()) } doReturn flowOf(Result.success(Request()))
        }

        sut = SupportRequestFormViewModel(
            zendeskHelper = zendeskHelper,
            selectedSite = selectedSite,
            savedState = savedState
        )
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
        sut.onHelpOptionSelected(HelpOption.MobileApp)

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
        sut.onHelpOptionSelected(HelpOption.MobileApp)

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
        sut.onHelpOptionSelected(HelpOption.MobileApp)
        sut.onSubmitRequestButtonClicked(mock(), HelpOrigin.LOGIN_HELP_NOTIFICATION, emptyList())

        // Then
        assertThat(isSubmitButtonEnabled).hasSize(4)
        assertThat(isSubmitButtonEnabled[0]).isFalse
        assertThat(isSubmitButtonEnabled[1]).isTrue
        assertThat(isSubmitButtonEnabled[2]).isFalse
        assertThat(isSubmitButtonEnabled[3]).isTrue
    }
}
