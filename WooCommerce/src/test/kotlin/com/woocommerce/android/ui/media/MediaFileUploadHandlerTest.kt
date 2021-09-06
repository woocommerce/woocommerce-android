package com.woocommerce.android.ui.media

import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MediaFileUploadHandlerTest : BaseUnitTest() {
    private val resources: ResourceProvider = mock()
    private val productImagesServiceWrapper: ProductImagesServiceWrapper = mock()
    private lateinit var mediaFileUploadHandler: MediaFileUploadHandler

    @Before
    fun setup() {
        mediaFileUploadHandler = spy(MediaFileUploadHandler(resources, productImagesServiceWrapper, GlobalScope))
    }

    @Test
    fun `Handles product image upload error correctly`() {
//        assertThat(mediaFileUploadHandler.getMediaUploadErrorCount(testMediaModel.postId)).isEqualTo(0)
//
//        mediaFileUploadHandler.handleMediaUploadFailure(testMediaModel, testMediaModelError)
//        assertThat(mediaFileUploadHandler.getMediaUploadErrorCount(testMediaModel.postId)).isEqualTo(1)
//        assertThat(mediaFileUploadHandler.getMediaUploadErrorMessage(testMediaModel.postId)).isEqualTo(
//            resources.getString(R.string.product_image_service_error_uploading_single)
//        )
    }
}
