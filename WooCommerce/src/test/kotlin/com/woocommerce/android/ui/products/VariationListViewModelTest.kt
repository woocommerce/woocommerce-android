package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.ui.products.variations.VariationListViewModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class VariationListViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val variationRepository: VariationRepository = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val productRepository: ProductDetailRepository = mock()

    private val productRemoteId = 1L
    private lateinit var viewModel: VariationListViewModel
    private val variations = ProductTestUtils.generateProductVariationList(productRemoteId)
    private val savedState = SavedStateHandle()

    @Before
    fun setup() {
        doReturn(true).whenever(networkStatus).isConnected()
        whenever(productRepository.getProduct(productRemoteId)).thenReturn(mock())
    }

    private fun createViewModel() {
        viewModel = spy(
            VariationListViewModel(
                    savedState,
                    variationRepository,
                    productRepository,
                    networkStatus,
                    currencyFormatter
            )
        )
    }

    @Test
    fun `Displays the product variation list view correctly`() {
        doReturn(variations).whenever(variationRepository).getProductVariationList(productRemoteId)

        createViewModel()

        val fetchedVariationList = ArrayList<ProductVariation>()
        viewModel.variationList.observeForever { it?.let { fetchedVariationList.addAll(it) } }

        viewModel.start(productRemoteId)
        assertThat(fetchedVariationList).isEqualTo(variations)
    }

    @Test
    fun `Do not fetch product variations from api when not connected`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            doReturn(false).whenever(networkStatus).isConnected()

            createViewModel()

            var snackbar: ShowSnackbar? = null
            viewModel.event.observeForever {
                if (it is ShowSnackbar) snackbar = it
            }

            viewModel.start(productRemoteId)

            verify(variationRepository, times(1)).getProductVariationList(productRemoteId)
            verify(variationRepository, times(0)).fetchProductVariations(productRemoteId)

            assertThat(snackbar).isEqualTo(ShowSnackbar(string.offline_error))
        }

    @Test
    fun `Shows and hides product variations skeleton correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(emptyList<ProductVariation>())
            .whenever(variationRepository).getProductVariationList(productRemoteId)

        createViewModel()

        val isSkeletonShown = ArrayList<Boolean>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { isSkeletonShown.add(it) }
        }

        viewModel.start(productRemoteId)
        assertThat(isSkeletonShown).containsExactly(true, false)
    }

    @Test
    fun `Display empty view on fetch product variations error`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        whenever(variationRepository.fetchProductVariations(productRemoteId)).thenReturn(null)
        whenever(variationRepository.getProductVariationList(productRemoteId)).thenReturn(null)

        createViewModel()

        val showEmptyView = ArrayList<Boolean>()
        viewModel.viewStateLiveData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showEmptyView.add(it) }
        }

        viewModel.start(productRemoteId)

        verify(variationRepository, times(1)).fetchProductVariations(productRemoteId)
        assertThat(showEmptyView).containsExactly(true, false)
    }
}
