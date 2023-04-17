package com.woocommerce.android.ui.login.storecreation

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.STORE_LOADING_FAILED
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.STORE_NOT_READY
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Failure
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Success
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.StoreCreationLoadingState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.SuccessState
import com.woocommerce.android.ui.login.storecreation.installation.StoreCreationLoadingCountDownTimer
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.mockito.internal.verification.Times
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class InstallationViewModelTest : BaseUnitTest() {
    private val savedState = SavedStateHandle()
    private val repository: StoreCreationRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val selectedSite: SelectedSite = mock()
    private val storeCreationLoadingCountDownTimer: StoreCreationLoadingCountDownTimer = mock()

    private lateinit var viewModel: InstallationViewModel

    private var observer: Observer<ViewState> = mock()

    private companion object {
        const val SITE_ID = 123L
        val INITIAL_LOADING_STATE = StoreCreationLoadingState(
            progress = 0F,
            title = string.store_creation_in_progress_title_1,
            description = string.store_creation_in_progress_description_1
        )
    }

    private fun whenViewModelIsCreated() {
        viewModel = InstallationViewModel(
            savedState,
            repository,
            newStore,
            analyticsTrackerWrapper,
            appPrefsWrapper,
            selectedSite,
            storeCreationLoadingCountDownTimer
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

    @Before
    fun setup() {
        whenever(storeCreationLoadingCountDownTimer.observe()).thenReturn(flowOf(INITIAL_LOADING_STATE))
    }

    @Test
    fun `when a Woo site is found after installation, a success state is displayed and loading canceled`() =
        testBlocking {
            whenever(repository.fetchSiteAfterCreation(newStore.data.siteId!!)).thenReturn(Success(Unit))
            whenever(selectedSite.get()).thenReturn(SiteModel().apply { url = newStore.data.domain })

            whenViewModelIsCreated()

            viewModel.viewState.observeForever(observer)
            advanceUntilIdle()

            verify(storeCreationLoadingCountDownTimer).cancelTimer()
            val expectedState = SuccessState(newStore.data.domain!!.slashJoin("wp-admin/"))
            assertEquals(expectedState, viewModel.viewState.value)
        }

    @Test
    fun `when a site is found but not ready after 10 tries, an error state is displayed and timer is cancelled`() =
        testBlocking {
            whenever(repository.fetchSiteAfterCreation(newStore.data.siteId!!)).thenReturn(Failure(STORE_NOT_READY))

            whenViewModelIsCreated()

            viewModel.viewState.observeForever(observer)
            advanceUntilIdle()

            verify(repository, Times(10)).fetchSiteAfterCreation(any())
            verify(storeCreationLoadingCountDownTimer).cancelTimer()
            val expectedState = ErrorState(STORE_NOT_READY)
            assertEquals(expectedState, viewModel.viewState.value)
        }

    @Test
    fun `when a site fetching returns an error, the flow fails immediately`() = testBlocking {
        whenever(repository.fetchSiteAfterCreation(newStore.data.siteId!!)).thenReturn(Failure(STORE_LOADING_FAILED))

        whenViewModelIsCreated()

        viewModel.viewState.observeForever(observer)
        advanceUntilIdle()

        verify(repository, Times(1)).fetchSiteAfterCreation(any())
        verify(storeCreationLoadingCountDownTimer).cancelTimer()
        val expectedState = ErrorState(STORE_LOADING_FAILED)
        assertEquals(expectedState, viewModel.viewState.value)
    }

    @Test
    fun `when viewmodel is created, loading timer is initiated`() =
        testBlocking {
            whenViewModelIsCreated()

            verify(storeCreationLoadingCountDownTimer).startTimer()
        }
}
