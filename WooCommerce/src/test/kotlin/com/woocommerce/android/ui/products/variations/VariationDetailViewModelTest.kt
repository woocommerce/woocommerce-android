package com.woocommerce.android.ui.products.variations

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.media.MediaFileUploadHandler.ProductImageUploadData
import com.woocommerce.android.ui.media.MediaFileUploadHandler.UploadStatus
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.generateVariation
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.variations.VariationDetailViewModel.HideImageUploadErrorSnackbar
import com.woocommerce.android.ui.products.variations.VariationDetailViewModel.VariationViewState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowActionSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnVariationChanged
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

@ExperimentalCoroutinesApi
class VariationDetailViewModelTest : BaseUnitTest() {
    companion object {
        private val SALE_START_DATE = Date.from(
            LocalDateTime.of(2020, 3, 1, 8, 0).toInstant(ZoneOffset.UTC)
        )
        private val SALE_END_DATE = Date.from(
            LocalDateTime.of(2020, 4, 1, 8, 0).toInstant(ZoneOffset.UTC)
        )
        private val DUMMY_REGULAR_PRICE = BigDecimal(99)
        private val DUMMY_SALE_PRICE = BigDecimal(70)
        val TEST_VARIATION = generateVariation().copy(
            saleStartDateGmt = SALE_START_DATE,
            saleEndDateGmt = SALE_END_DATE,
            regularPrice = DUMMY_REGULAR_PRICE,
            salePrice = DUMMY_SALE_PRICE,
        )
    }

    private lateinit var sut: VariationDetailViewModel

    private val siteParams = SiteParameters(
        currencyCode = "USD",
        currencySymbol = "$",
        currencyFormattingParameters = null,
        weightUnit = "kg",
        dimensionUnit = "cm",
        gmtOffset = 0f
    )
    private val parameterRepository: ParameterRepository = mock {
        on { getParameters(any(), any<SavedStateHandle>()) } doReturn (siteParams)
    }
    private val variationRepository: VariationDetailRepository = mock {
        onBlocking { getVariation(any(), any()) } doReturn TEST_VARIATION
        onBlocking { fetchVariation(any(), any()) } doAnswer {
            OnVariationChanged(it.arguments[0] as Long, it.arguments[1] as Long)
        }
    }

    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { answer -> answer.arguments[0].toString() }
    }

    private val networkStatus: NetworkStatus = mock {
        on { isConnected() } doReturn true
    }

    private val mediaFileUploadHandler: MediaFileUploadHandler = mock {
        on { it.observeCurrentUploadErrors(any()) } doReturn emptyFlow()
        on { it.observeCurrentUploads(any()) } doReturn flowOf(emptyList())
        on { it.observeSuccessfulUploads(any()) } doReturn emptyFlow()
    }

    private val savedState = VariationDetailFragmentArgs(
        TEST_VARIATION.remoteProductId,
        TEST_VARIATION.remoteVariationId
    ).toSavedStateHandle()

    @Before
    fun setup() {
        sut = VariationDetailViewModel(
            savedState = savedState,
            variationRepository = variationRepository,
            productRepository = mock(),
            networkStatus = networkStatus,
            currencyFormatter = mock(),
            parameterRepository = parameterRepository,
            resources = resourceProvider,
            mediaFileUploadHandler = mediaFileUploadHandler
        )
    }

    @Test
    fun `should initialize sale end date`() {
        sut.variationViewStateData.observeForever { _, _ -> }

        assertThat(variation?.saleEndDateGmt).isEqualTo(SALE_END_DATE)
    }

    @Test
    fun `should initialize sale start date`() {
        sut.variationViewStateData.observeForever { _, _ -> }

        assertThat(variation?.saleStartDateGmt).isEqualTo(SALE_START_DATE)
    }

    @Test
    fun `should update sale end date to null when requested`() {
        sut.variationViewStateData.observeForever { _, _ -> }

        sut.onVariationChanged(saleEndDate = null)

        assertThat(variation!!.saleEndDateGmt).isNull()
    }

    @Test
    fun `should initialize uploading image uri with null value`() {
        sut.variationViewStateData.observeForever { _, _ -> }

        assertThat(viewState!!.uploadingImageUri).isNull()
    }

    @Test
    fun `when there image upload errors, then show a snackbar`() = testBlocking {
        val errorEvents = MutableSharedFlow<List<ProductImageUploadData>>()
        doReturn(errorEvents).whenever(mediaFileUploadHandler)
            .observeCurrentUploadErrors(TEST_VARIATION.remoteVariationId)
        val errorMessage = "message"
        doReturn(errorMessage).whenever(resourceProvider).getString(any(), anyVararg())

        setup()
        val errors = listOf(
            ProductImageUploadData(
                TEST_VARIATION.remoteVariationId,
                "uri",
                UploadStatus.Failed(
                    mediaErrorType = GENERIC_ERROR,
                    mediaErrorMessage = "error"
                )
            )
        )
        errorEvents.emit(errors)

        assertThat(sut.event.value).matches {
            it is ShowActionSnackbar &&
                it.message == errorMessage
        }
    }

    @Test
    fun `Display error message on min-max quantities update product error`() = testBlocking {
        val displayErrorMessage = "This is an error message"
        var result = WCProductStore.OnVariationUpdated(1, 1, 2)
        result.error = WCProductStore.ProductError(
            type = WCProductStore.ProductErrorType.INVALID_MIN_MAX_QUANTITY,
            message = displayErrorMessage
        )
        doReturn(result).whenever(variationRepository).updateVariation(any())

        setup()

        var showUpdateProductError: VariationDetailViewModel.ShowUpdateVariationError? = null
        sut.event.observeForever {
            if (it is VariationDetailViewModel.ShowUpdateVariationError) showUpdateProductError = it
        }

        sut.onUpdateButtonClicked()

        Assertions.assertThat(showUpdateProductError?.message)
            .isEqualTo(displayErrorMessage)
    }

    @Test
    fun `when image uploads gets cleared, then auto-dismiss the snackbar`() = testBlocking {
        val errorEvents = MutableSharedFlow<List<ProductImageUploadData>>()
        doReturn(errorEvents).whenever(mediaFileUploadHandler)
            .observeCurrentUploadErrors(TEST_VARIATION.remoteVariationId)

        setup()
        errorEvents.emit(emptyList())

        assertThat(sut.event.value).isEqualTo(HideImageUploadErrorSnackbar)
    }

    @Test
    fun `given regular price set, when updating attributes, then price remains unchanged`() = testBlocking {
        sut.variationViewStateData.observeForever { _, _ -> }

        sut.onVariationChanged(attributes = emptyArray())

        assertThat(variation!!.regularPrice).isEqualTo(DUMMY_REGULAR_PRICE)
    }

    @Test
    fun `given sale price set, when updating attributes, then price remains unchanged`() = testBlocking {
        sut.variationViewStateData.observeForever { _, _ -> }

        sut.onVariationChanged(attributes = emptyArray())

        assertThat(variation!!.salePrice).isEqualTo(DUMMY_SALE_PRICE)
    }

    @Test
    fun `given regular price greater than 0, when setting price to 0, then price is set to zero`() = testBlocking {
        sut.variationViewStateData.observeForever { _, _ -> }

        sut.onVariationChanged(regularPrice = BigDecimal(0))

        assertThat(variation!!.regularPrice).isEqualTo(BigDecimal(0))
    }

    @Test
    fun `given sale price greater than 0, when setting price to 0, then price is set to zero`() = testBlocking {
        sut.variationViewStateData.observeForever { _, _ -> }

        sut.onVariationChanged(salePrice = BigDecimal(0))

        assertThat(variation!!.salePrice).isEqualTo(BigDecimal(0))
    }

    @Test
    fun `given regular price greater than 0, when setting price to null, then price is set to null`() = testBlocking {
        sut.variationViewStateData.observeForever { _, _ -> }

        sut.onVariationChanged(regularPrice = null)

        assertThat(variation!!.regularPrice).isNull()
    }

    @Test
    fun `given sale price greater than 0, when setting price to null, then price is set to null`() = testBlocking {
        sut.variationViewStateData.observeForever { _, _ -> }

        sut.onVariationChanged(salePrice = null)

        assertThat(variation!!.salePrice).isNull()
    }

    private val variation: ProductVariation?
        get() = viewState?.variation

    private val viewState: VariationViewState?
        get() = sut.variationViewStateData.liveData.value
}
