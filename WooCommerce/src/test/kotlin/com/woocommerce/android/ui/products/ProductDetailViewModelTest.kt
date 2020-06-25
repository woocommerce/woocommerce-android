package com.woocommerce.android.ui.products

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductImagesViewState
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.models.ProductProperty.ComplexProperty
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import com.woocommerce.android.ui.products.models.ProductProperty.Editable
import com.woocommerce.android.ui.products.models.ProductProperty.Link
import com.woocommerce.android.ui.products.models.ProductProperty.PropertyGroup
import com.woocommerce.android.ui.products.models.ProductProperty.RatingBar
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.PRIMARY
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.SECONDARY
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class ProductDetailViewModelTest : BaseUnitTest() {
    companion object {
        private const val PRODUCT_REMOTE_ID = 1L
        private const val OFFLINE_PRODUCT_REMOTE_ID = 2L
        private const val TEST_STRING = "test"
    }

    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val productRepository: ProductDetailRepository = mock()
    private val productCategoriesRepository: ProductCategoriesRepository = mock()
    private val resources: ResourceProvider = mock()
    private val productImagesServiceWrapper: ProductImagesServiceWrapper = mock()
    private val currencyFormatter: CurrencyFormatter = mock {
        on(it.formatCurrency(any<BigDecimal>(), any(), any())).thenAnswer { i -> "${i.arguments[1]}${i.arguments[0]}" }
    }
    private val savedState: SavedStateWithArgs = mock()

    @ExperimentalCoroutinesApi
    private val coroutineDispatchers = CoroutineDispatchers(
        TestCoroutineDispatcher(), TestCoroutineDispatcher(), TestCoroutineDispatcher()
    )
    private val product = ProductTestUtils.generateProduct(PRODUCT_REMOTE_ID)
    private val offlineProduct = ProductTestUtils.generateProduct(OFFLINE_PRODUCT_REMOTE_ID)
    private val productCategories = ProductTestUtils.generateProductCategories()
    private lateinit var viewModel: ProductDetailViewModel

    private val productWithParameters = ProductDetailViewState(
            productDraft = product,
            storedProduct = product,
            productBeforeEnteringFragment = product,
            isSkeletonShown = false,
            uploadingImageUris = null,
            weightWithUnits = "10kg",
            sizeWithUnits = "1 x 2 x 3 cm",
            salePriceWithCurrency = "CZK10.00",
            regularPriceWithCurrency = "CZK30.00"
    )

    private val expectedCards = listOf(
        ProductPropertyCard(
            type = PRIMARY,
            properties = listOf(
                Editable(R.string.product_detail_title_hint, product.name),
                ComplexProperty(R.string.product_description, product.description),
                RatingBar(R.string.product_reviews, product.ratingCount.toString(), product.averageRating)
            )
        ),
        ProductPropertyCard(
            type = SECONDARY,
            properties = listOf(
                PropertyGroup(
                    R.string.product_price,
                    mapOf(
                        TEST_STRING to productWithParameters.regularPriceWithCurrency!!
                    ),
                    R.drawable.ic_gridicons_money
                ),
                PropertyGroup(
                    R.string.product_inventory,
                    mapOf(
                        "" to TEST_STRING
                    ),
                    R.drawable.ic_gridicons_list_checkmark,
                    false
                ),
                ComplexProperty(
                    R.string.product_short_description,
                    TEST_STRING,
                    R.drawable.ic_gridicons_align_left,
                    true
                )
            )
        )
    )

    @Before
    fun setup() {
        doReturn(MutableLiveData(ProductDetailViewState()))
            .whenever(savedState).getLiveData<ProductDetailViewState>(any(), any())
        doReturn(MutableLiveData(ProductImagesViewState()))
            .whenever(savedState).getLiveData<ProductImagesViewState>(any(), any())

        // Avoids the unnecessary stubbing exception
        if (BuildConfig.DEBUG) {
            doReturn(TEST_STRING).whenever(resources).getString(any())
        }

        viewModel = spy(
            ProductDetailViewModel(
                savedState,
                coroutineDispatchers,
                selectedSite,
                productRepository,
                networkStatus,
                currencyFormatter,
                wooCommerceStore,
                productImagesServiceWrapper,
                resources,
                productCategoriesRepository
            )
        )
        val prodSettings = WCProductSettingsModel(0).apply {
            dimensionUnit = "cm"
            weightUnit = "kg"
        }
        val siteSettings = mock<WCSettingsModel> {
            on(it.currencyCode).thenReturn("CZK")
        }

        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(prodSettings).whenever(wooCommerceStore).getProductSettings(any())
        doReturn(siteSettings).whenever(wooCommerceStore).getSiteSettings(any())
    }

    @Test
    fun `Displays the product detail properties correctly`() = runBlockingTest {
        // This feature is only enabled in debug mode and would thus fail in release mode
        if (BuildConfig.DEBUG) {
            doReturn(product).whenever(productRepository).getProduct(any())

            viewModel.productDetailViewStateData.observeForever { _, _ -> }

            var cards: List<ProductPropertyCard>? = null
            viewModel.productDetailCards.observeForever {
                cards = it.map { card -> stripCallbacks(card) }
            }

            viewModel.start(PRODUCT_REMOTE_ID)

            assertThat(expectedCards).isEqualTo(cards)
        }
    }

    private fun stripCallbacks(card: ProductPropertyCard): ProductPropertyCard {
        return card.copy(properties = card.properties.map { p ->
            when (p) {
                is ComplexProperty -> p.copy(onClick = null)
                is Editable -> p.copy(onTextChanged = null)
                is PropertyGroup -> p.copy(onClick = null)
                is Link -> p.copy(onClick = null)
                else -> p
            }
        })
    }

    @Test
    fun `Displays the product detail view correctly`() {
        doReturn(product).whenever(productRepository).getProduct(any())

        var productData: ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        assertThat(productData).isEqualTo(ProductDetailViewState())

        viewModel.start(PRODUCT_REMOTE_ID)

        assertThat(productData).isEqualTo(productWithParameters)
    }

    @Test
    fun `Display error message on fetch product error`() = runBlockingTest {
        whenever(productRepository.fetchProduct(PRODUCT_REMOTE_ID)).thenReturn(null)
        whenever(productRepository.getProduct(PRODUCT_REMOTE_ID)).thenReturn(null)

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start(PRODUCT_REMOTE_ID)

        verify(productRepository, times(1)).fetchProduct(PRODUCT_REMOTE_ID)

        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.product_detail_fetch_product_error))
    }

    @Test
    fun `Do not fetch product from api when not connected`() = runBlockingTest {
        doReturn(offlineProduct).whenever(productRepository).getProduct(any())
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start(PRODUCT_REMOTE_ID)

        verify(productRepository, times(1)).getProduct(PRODUCT_REMOTE_ID)
        verify(productRepository, times(0)).fetchProduct(any())

        assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
    }

    @Test
    fun `Shows and hides product detail skeleton correctly`() = runBlockingTest {
        doReturn(null).whenever(productRepository).getProduct(any())

        val isSkeletonShown = ArrayList<Boolean>()
        viewModel.productDetailViewStateData.observeForever { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { isSkeletonShown.add(it) }
        }

        viewModel.start(PRODUCT_REMOTE_ID)

        assertThat(isSkeletonShown).containsExactly(true, false)
    }

    @Test
    fun `Displays the updated product detail view correctly`() {
        doReturn(product).whenever(productRepository).getProduct(any())

        var productData: ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        assertThat(productData).isEqualTo(ProductDetailViewState())

        viewModel.start(PRODUCT_REMOTE_ID)
        assertThat(productData).isEqualTo(productWithParameters)

        val updatedDescription = "Updated product description"
        viewModel.updateProductDraft(updatedDescription)

        viewModel.start(PRODUCT_REMOTE_ID)
        assertThat(productData?.productDraft?.description).isEqualTo(updatedDescription)
    }

    @Test
    fun `Displays update menu action if product is edited`() {
        doReturn(product).whenever(productRepository).getProduct(any())

        var productData: ProductDetailViewState? = null
        viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

        viewModel.start(PRODUCT_REMOTE_ID)
        assertThat(productData?.isProductUpdated).isNull()

        val updatedDescription = "Updated product description"
        viewModel.updateProductDraft(updatedDescription)

        viewModel.start(PRODUCT_REMOTE_ID)
        assertThat(productData?.isProductUpdated).isTrue()
    }

    @Test
    fun `Displays progress dialog when product is edited`() = runBlockingTest {
        // This feature is only enabled in debug mode and would thus fail in release mode
        if (BuildConfig.DEBUG) {
            doReturn(product).whenever(productRepository).getProduct(any())
            doReturn(false).whenever(productRepository).updateProduct(any())

            val isProgressDialogShown = ArrayList<Boolean>()
            viewModel.productDetailViewStateData.observeForever { old, new ->
                new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                    isProgressDialogShown.add(it)
                }
            }

            viewModel.start(PRODUCT_REMOTE_ID)
            viewModel.onUpdateButtonClicked()

            assertThat(isProgressDialogShown).containsExactly(true, false)
        }
    }

    @Test
    fun `Do not update product when not connected`() = runBlockingTest {
        // This feature is only enabled in debug mode and would thus fail in release mode
        if (BuildConfig.DEBUG) {
            doReturn(product).whenever(productRepository).getProduct(any())
            doReturn(false).whenever(networkStatus).isConnected()

            var snackbar: ShowSnackbar? = null
            viewModel.event.observeForever {
                if (it is ShowSnackbar) snackbar = it
            }

            var productData: ProductDetailViewState? = null
            viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

            viewModel.start(PRODUCT_REMOTE_ID)
            viewModel.onUpdateButtonClicked()

            verify(productRepository, times(0)).updateProduct(any())
            assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.offline_error))
            assertThat(productData?.isProgressDialogShown).isFalse()
        }
    }

    @Test
    fun `Display error message on update product error`() = runBlockingTest {
        // This feature is only enabled in debug mode and would thus fail in release mode
        if (BuildConfig.DEBUG) {
            doReturn(product).whenever(productRepository).getProduct(any())
            doReturn(false).whenever(productRepository).updateProduct(any())

            var snackbar: ShowSnackbar? = null
            viewModel.event.observeForever {
                if (it is ShowSnackbar) snackbar = it
            }

            var productData: ProductDetailViewState? = null
            viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

            viewModel.start(PRODUCT_REMOTE_ID)
            viewModel.onUpdateButtonClicked()

            verify(productRepository, times(1)).updateProduct(any())
            assertThat(snackbar).isEqualTo(ShowSnackbar(R.string.product_detail_update_product_error))
            assertThat(productData?.isProgressDialogShown).isFalse()
        }
    }

    @Test
    fun `Display success message on update product success`() = runBlockingTest {
        // This feature is only enabled in debug mode and would thus fail in release mode
        if (BuildConfig.DEBUG) {
            doReturn(product).whenever(productRepository).getProduct(any())
            doReturn(true).whenever(productRepository).updateProduct(any())

            var successSnackbarShown = false
            viewModel.event.observeForever {
                if (it is ShowSnackbar && it.message == R.string.product_detail_update_product_success) {
                    successSnackbarShown = true
                }
            }

            var productData: ProductDetailViewState? = null
            viewModel.productDetailViewStateData.observeForever { _, new -> productData = new }

            viewModel.start(PRODUCT_REMOTE_ID)
            viewModel.onUpdateButtonClicked()

            verify(productRepository, times(1)).updateProduct(any())
            verify(productRepository, times(2)).getProduct(PRODUCT_REMOTE_ID)

            assertThat(successSnackbarShown).isTrue()
            assertThat(productData?.isProgressDialogShown).isFalse()
            assertThat(productData?.isProductUpdated).isFalse()
            assertThat(productData?.productDraft).isEqualTo(product)
        }
    }

    @Test
    fun `Correctly sorts the Product Categories By their Parent Ids and by name`() = runBlockingTest {
        val sortedByNameAndParent = viewModel.sortAndStyleProductCategories(
            product, productCategories
        ).toList()
        assertThat(sortedByNameAndParent[0].category).isEqualTo(productCategories[0])
        assertThat(sortedByNameAndParent[1].category).isEqualTo(productCategories[7])
        assertThat(sortedByNameAndParent[2].category).isEqualTo(productCategories[10])
        assertThat(sortedByNameAndParent[3].category).isEqualTo(productCategories[1])
        assertThat(sortedByNameAndParent[4].category).isEqualTo(productCategories[6])
        assertThat(sortedByNameAndParent[5].category).isEqualTo(productCategories[8])
        assertThat(sortedByNameAndParent[6].category).isEqualTo(productCategories[9])
        assertThat(sortedByNameAndParent[7].category).isEqualTo(productCategories[2])
        assertThat(sortedByNameAndParent[8].category).isEqualTo(productCategories[3])
        assertThat(sortedByNameAndParent[9].category).isEqualTo(productCategories[5])
        assertThat(sortedByNameAndParent[10].category).isEqualTo(productCategories[4])
    }
}
