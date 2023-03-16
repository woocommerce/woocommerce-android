package com.woocommerce.android.ui.login.storecreation

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.STORE_LOADING_FAILED
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.STORE_NOT_READY
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Failure
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Success
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.SuccessState
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.mockito.internal.verification.Times
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
class InstallationViewModelTest : BaseUnitTest() {
    private val savedState = SavedStateHandle()
    private val repository: StoreCreationRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val selectedSite: SelectedSite = mock()

    private lateinit var viewModel: InstallationViewModel

    private var observer: Observer<ViewState> = mock()
    private val captor = argumentCaptor<ViewState>()

    companion object {
        private const val SITE_ID = 123L
    }

    private fun whenViewModelIsCreated() {
        viewModel = InstallationViewModel(
            savedState,
            repository,
            newStore,
            analyticsTrackerWrapper,
            appPrefsWrapper,
            selectedSite
        )
    }

    private val newStore = NewStore().also {
        it.update(
            name = "eCommerce",
            domain = "woowoozela.com",
            siteId = SITE_ID,
            planPathSlug = "ecommerce_bundle",
            planProductId = 321
        )
    }

    @Test
    fun `when a Woo site is found after installation, a success state is displayed`() = testBlocking {
        whenever(repository.fetchSiteAfterCreation(newStore.data.siteId!!)).thenReturn(Success(Unit))
        whenever(selectedSite.get()).thenReturn(SiteModel().apply { url = newStore.data.domain })

        whenViewModelIsCreated()

        viewModel.viewState.observeForever(observer)
        advanceUntilIdle()

        verify(observer, Times(3)).onChanged(captor.capture())

        val observedState = captor.lastValue as? SuccessState
        assertNotNull(observedState)

        verify(repository, Times(1)).fetchSiteAfterCreation(newStore.data.siteId!!)

        val expectedState = SuccessState(newStore.data.domain!!)

        assertEquals(expectedState, observedState)
    }

    @Test
    fun `when a site is found but not ready after 10 tries, an error state is displayed`() = testBlocking {
        whenever(repository.fetchSiteAfterCreation(newStore.data.siteId!!)).thenReturn(Failure(STORE_NOT_READY))

        whenViewModelIsCreated()

        viewModel.viewState.observeForever(observer)
        advanceUntilIdle()

        verify(observer, Times(3)).onChanged(captor.capture())

        val observedState = captor.lastValue as? ErrorState
        assertNotNull(observedState)

        verify(repository, Times(10)).fetchSiteAfterCreation(any())

        val expectedState = ErrorState(STORE_NOT_READY)

        assertEquals(expectedState, observedState)
    }

    @Test
    fun `when a site fetching returns an error, the flow fails immediately`() = testBlocking {
        whenever(repository.fetchSiteAfterCreation(newStore.data.siteId!!)).thenReturn(Failure(STORE_LOADING_FAILED))

        whenViewModelIsCreated()

        viewModel.viewState.observeForever(observer)
        advanceUntilIdle()

        verify(observer, Times(3)).onChanged(captor.capture())

        val observedState = captor.lastValue as? ErrorState
        assertNotNull(observedState)

        verify(repository, Times(1)).fetchSiteAfterCreation(any())

        val expectedState = ErrorState(STORE_LOADING_FAILED)

        assertEquals(expectedState, observedState)
    }
}
