package com.woocommerce.android.ui.login.storecreation.webview

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.webview.WebViewStoreCreationViewModel.ViewState
import com.woocommerce.android.ui.login.storecreation.webview.WebViewStoreCreationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.webview.WebViewStoreCreationViewModel.ViewState.StoreCreationState
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent

@ExperimentalCoroutinesApi
class WebViewStoreCreationViewModelTest : BaseUnitTest() {
    private val savedState = SavedStateHandle()
    private val repository: StoreCreationRepository = mock()
    private val wpComWebViewAuthenticator: WPComWebViewAuthenticator = mock()
    private val userAgent: UserAgent = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private lateinit var viewModel: WebViewStoreCreationViewModel
    private var observer: Observer<ViewState> = mock()
    private val captor = argumentCaptor<ViewState>()

    private val sites = listOf(
        SiteModel().apply {
            url = "https://cnn.com"
        },
        SiteModel().apply {
            url = "https://automattic.com"
            setIsJetpackConnected(true)
            setIsJetpackInstalled(true)
        }
    )

    private fun whenViewModelIsCreated() {
        viewModel = WebViewStoreCreationViewModel(
            savedState,
            wpComWebViewAuthenticator,
            userAgent,
            repository,
            analyticsTrackerWrapper,
            appPrefsWrapper
        )
    }

    @Before
    fun setup() = testBlocking {
        givenFetchSitesAfterCreation(sites)
        givenGetSiteBySiteUrl()
    }

    @Test
    fun `when view model is created, store creation step is shown`() = testBlocking {
        whenViewModelIsCreated()

        viewModel.viewState.observeForever(observer)
        verify(observer).onChanged(captor.capture())

        assertThat(captor.firstValue is StoreCreationState).isTrue
    }

    @Test
    fun `when a site address is found, it is added to the list`() = testBlocking {
        whenViewModelIsCreated()

        viewModel.viewState.observeForever(observer)
        verify(observer).onChanged(captor.capture())

        val site = sites.first()

        (captor.firstValue as StoreCreationState).apply {
            onSiteAddressFound(site.url)
            onStoreCreated()
        }

        verify(repository, Mockito.times(10)).getSiteBySiteUrl(site.url)
    }

    @Test
    fun `when a JP site is found, it is selected`() = testBlocking {
        whenViewModelIsCreated()

        viewModel.viewState.observeForever(observer)
        verify(observer).onChanged(captor.capture())

        val site = sites[1]

        (captor.firstValue as StoreCreationState).apply {
            onSiteAddressFound(site.url)
            onStoreCreated()
        }

        verify(repository).selectSite(site)
    }

    @Test
    fun `when a site fetch fails, an error state is shown`() = testBlocking {
        whenever(repository.fetchSitesAfterCreation()).thenReturn(Result.failure(Exception()))
        whenViewModelIsCreated()

        viewModel.viewState.observeForever(observer)
        verify(observer).onChanged(captor.capture())

        (captor.firstValue as StoreCreationState).apply {
            onStoreCreated()
        }

        verify(repository, Mockito.times(1)).fetchSitesAfterCreation()
        verify(observer, Mockito.times(3)).onChanged(captor.capture())

        assertThat(captor.lastValue is ErrorState).isTrue
    }

    @Test
    fun `when a maximum retries count reached, an error state is shown`() = testBlocking {
        whenViewModelIsCreated()

        viewModel.viewState.observeForever(observer)
        verify(observer).onChanged(captor.capture())

        (captor.firstValue as StoreCreationState).apply {
            onStoreCreated()
        }

        verify(repository, Mockito.times(10)).fetchSitesAfterCreation()
        verify(observer, Mockito.times(3)).onChanged(captor.capture())

        assertThat(captor.lastValue is ErrorState).isTrue
    }

    private suspend fun givenFetchSitesAfterCreation(sites: List<SiteModel>) {
        whenever(repository.fetchSitesAfterCreation()).thenReturn(Result.success(sites))
    }

    private fun givenGetSiteBySiteUrl() {
        whenever(repository.getSiteBySiteUrl(sites[0].url)).thenReturn(
            sites[0]
        )
        whenever(repository.getSiteBySiteUrl(sites[1].url)).thenReturn(
            sites[1]
        )
    }
}
