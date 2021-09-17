package com.woocommerce.android.ui.products.variations

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.initSavedStateHandle
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.GENERIC_ERROR
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@RunWith(RobolectricTestRunner::class)
class VariationDetailViewModelTest : BaseUnitTest() {
    companion object {
        private val SALE_START_DATE = Date.from(
            LocalDateTime.of(2020, 3, 1, 8, 0).toInstant(ZoneOffset.UTC)
        )
        private val SALE_END_DATE = Date.from(
            LocalDateTime.of(2020, 4, 1, 8, 0).toInstant(ZoneOffset.UTC)
        )
        val TEST_VARIATION = generateVariation().copy(
            saleStartDateGmt = SALE_START_DATE,
            saleEndDateGmt = SALE_END_DATE
        )
    }

    private lateinit var sut: VariationDetailViewModel

    private val siteParams = SiteParameters(
        currencyCode = "USD",
        currencySymbol = "$",
        currencyPosition = null,
        weightUnit = "kg",
        dimensionUnit = "cm",
        gmtOffset = 0f
    )
    private val parameterRepository: ParameterRepository = mock {
        on { getParameters(any(), any<SavedStateHandle>()) } doReturn (siteParams)
    }
    private val variationRepository: VariationDetailRepository = mock {
        on { getVariation(any(), any()) } doReturn TEST_VARIATION
        onBlocking { fetchVariation(any(), any()) } doReturn TEST_VARIATION
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
    ).initSavedStateHandle()

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
        doReturn(errorMessage).whenever(resourceProvider).getString(any())
        doReturn(errorMessage).whenever(resourceProvider).getString(any(), anyVararg())

        setup()
        val errors = listOf(
            ProductImageUploadData(
                TEST_VARIATION.remoteVariationId,
                "uri",
                UploadStatus.Failed(
                    MediaModel(),
                    GENERIC_ERROR,
                    "error"
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
    fun `when image uploads gets cleared, then auto-dismiss the snackbar`() = testBlocking {
        val errorEvents = MutableSharedFlow<List<ProductImageUploadData>>()
        doReturn(errorEvents).whenever(mediaFileUploadHandler)
            .observeCurrentUploadErrors(TEST_VARIATION.remoteVariationId)

        setup()
        errorEvents.emit(emptyList())

        assertThat(sut.event.value).isEqualTo(HideImageUploadErrorSnackbar)
    }

    private val variation: ProductVariation?
        get() = viewState?.variation

    private val viewState: VariationViewState?
        get() = sut.variationViewStateData.liveData.value
}
