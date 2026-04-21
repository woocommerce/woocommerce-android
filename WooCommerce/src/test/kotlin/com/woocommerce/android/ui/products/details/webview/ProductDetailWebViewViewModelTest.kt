package com.woocommerce.android.ui.products.details.webview

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent

@ExperimentalCoroutinesApi
class ProductDetailWebViewViewModelTest : BaseUnitTest() {

    private val productDetailRepository: ProductDetailRepository = mock()
    private val selectedSite: SelectedSite = mock()
    private val webViewAuthenticator: WebViewAuthenticator = mock()
    private val userAgent: UserAgent = mock()

    private lateinit var viewModel: ProductDetailWebViewViewModel
    private lateinit var savedStateHandle: SavedStateHandle

    private val testProduct = ProductTestUtils.generateProduct(
        productId = 456L,
        productName = "Test Product"
    )

    private val testSite = SiteModel().apply {
        adminUrl = "https://example.com/wp-admin/"
    }

    @Before
    fun setup() {
        whenever(selectedSite.get()).doReturn(testSite)
    }

    private suspend fun setupViewModel(
        remoteProductId: Long = testProduct.remoteId,
        setupMocks: suspend () -> Unit = {}
    ) {
        setupMocks()

        savedStateHandle = SavedStateHandle().apply {
            set("remoteProductId", remoteProductId)
        }

        viewModel = ProductDetailWebViewViewModel(
            savedStateHandle = savedStateHandle,
            productDetailRepository = productDetailRepository,
            selectedSite = selectedSite,
            webViewAuthenticator = webViewAuthenticator,
            userAgent = userAgent
        )
    }

    @Test
    fun `given valid product, when viewState is observed, then correct ViewState is emitted`() = testBlocking {
        // GIVEN
        setupViewModel {
            whenever(productDetailRepository.getProductAsync(testProduct.remoteId))
                .doReturn(testProduct)
        }

        // WHEN
        val viewState = viewModel.viewState.getOrAwaitValue()

        // THEN
        assertThat(viewState.title).isEqualTo(testProduct.name)
        assertThat(viewState.urlToLoad).isEqualTo(
            "https://example.com/wp-admin/${ProductDetailWebViewViewModel.BOOKABLE_SERVICE_PATH}" +
                "/${testProduct.remoteId}"
        )
    }

    @Test
    fun `given product not found, when viewState is observed, then navigation back event is triggered`() = testBlocking {
        // GIVEN
        setupViewModel {
            whenever(productDetailRepository.getProductAsync(testProduct.remoteId))
                .doReturn(null)
        }

        // WHEN
        val event = viewModel.event.runAndCaptureValues {
            // Trigger viewState observation
            viewModel.viewState.captureValues()
        }.last()

        // THEN
        assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
    }
}
