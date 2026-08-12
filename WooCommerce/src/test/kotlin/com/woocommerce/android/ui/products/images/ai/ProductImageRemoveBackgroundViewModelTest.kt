package com.woocommerce.android.ui.products.images.ai

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

@ExperimentalCoroutinesApi
class ProductImageRemoveBackgroundViewModelTest : BaseUnitTest() {

    private val performBackgroundRemoval: PerformImageBackgroundRemoval = mock()
    private val saveProcessedImage: SaveProcessedImageToTheProduct = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private val testImage = Product.Image(
        id = 1L,
        name = "test.jpg",
        alt = null,
        source = "https://example.com/test.jpg",
        dateCreated = Date(),
        isCoverImage = false
    )
    private val testProductId = 123L
    private val testBitmap: Bitmap = mock()

    private lateinit var viewModel: ProductImageRemoveBackgroundViewModel
    private lateinit var uriMockedStatic: MockedStatic<android.net.Uri>

    @Before
    fun setup() = testBlocking {
        uriMockedStatic = Mockito.mockStatic(android.net.Uri::class.java)
        uriMockedStatic.`when`<android.net.Uri> { android.net.Uri.parse(org.mockito.kotlin.any()) }.thenReturn(mock())

        val savedStateHandle = SavedStateHandle().apply {
            set("image", testImage)
            set("remoteProductId", testProductId)
        }

        whenever(performBackgroundRemoval.invoke(testImage.source)) doReturn Result.failure(Exception("Test"))

        viewModel = ProductImageRemoveBackgroundViewModel(
            savedState = savedStateHandle,
            performBackgroundRemoval = performBackgroundRemoval,
            saveProcessedImage = saveProcessedImage,
            analyticsTrackerWrapper = analyticsTrackerWrapper
        )

        (viewModel.state as MutableStateFlow).value = ViewState.Success(testBitmap)
    }

    @After
    fun tearDown() {
        uriMockedStatic.close()
    }

    @Test
    fun `when save copy button is tapped, then analytics event is tracked`() = testBlocking {
        // When
        viewModel.onSaveImageTapped()

        // Then
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PRODUCT_IMAGE_REMOVE_BACKGROUND_SAVE_BUTTON_TAPPED)
    }

    @Test
    fun `when background removal effect is discarded via back dialog, then analytics event is tracked`() {
        // When
        viewModel.onBackDialogConfirmed()

        // Then
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PRODUCT_IMAGE_REMOVE_BACKGROUND_DISCARDED)
    }

    @Test
    fun `when background removal effect is discarded via cancel, then analytics event is tracked`() {
        // When
        viewModel.onCancelClicked()

        // Then
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PRODUCT_IMAGE_REMOVE_BACKGROUND_DISCARDED)
    }
}
