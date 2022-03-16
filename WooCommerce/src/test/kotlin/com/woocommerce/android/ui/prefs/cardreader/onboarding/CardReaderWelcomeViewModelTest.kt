package com.woocommerce.android.ui.prefs.cardreader.onboarding

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.cardreader.onboarding.CardReaderWelcomeViewModel.CardReaderWelcomeDialogEvent.NavigateToOnboardingFlow
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class CardReaderWelcomeViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderWelcomeViewModel
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(SiteModel())
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private val savedState = CardReaderWelcomeDialogArgs(
        cardReaderFlowParam = CardReaderFlowParam.CardReadersHub
    ).initSavedStateHandle()

    @Before
    fun setUp() {
        viewModel = CardReaderWelcomeViewModel(savedState, selectedSite, appPrefsWrapper)
    }

    @Test
    fun `when screen shown, then view state initialized`() {
        assertThat(viewModel.viewState).isNotNull
    }

    @Test
    fun `when user clicks on continue, then the app navigates to onboarding flow`() {
        viewModel.viewState.value!!.buttonAction.invoke()

        assertThat(viewModel.event.value).isInstanceOf(NavigateToOnboardingFlow::class.java)
    }

    @Test
    fun `given onboarding completed, when view model created, then the app navigates to onboarding flow`() {
        whenever(appPrefsWrapper.isCardReaderOnboardingCompleted(anyInt(), anyLong(), anyLong()))
            .thenReturn(true)

        viewModel = CardReaderWelcomeViewModel(savedState, selectedSite, appPrefsWrapper)

        assertThat(viewModel.event.value).isInstanceOf(NavigateToOnboardingFlow::class.java)
    }

    @Test
    fun `given onboarding not completed, when view model created, then the app does not navigate to onboarding flow`() {
        whenever(appPrefsWrapper.isCardReaderOnboardingCompleted(anyInt(), anyLong(), anyLong()))
            .thenReturn(false)

        viewModel = CardReaderWelcomeViewModel(savedState, selectedSite, appPrefsWrapper)

        assertThat(viewModel.event.value).isNull()
    }
}
