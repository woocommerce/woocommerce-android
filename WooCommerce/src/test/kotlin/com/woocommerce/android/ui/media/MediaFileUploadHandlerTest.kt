package com.woocommerce.android.ui.media

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MediaFileUploadHandlerTest : BaseUnitTest() {
    companion object {
        private const val REMOTE_PRODUCT_ID = 1L
        private const val REMOTE_SITE_ID = 1L
    }

    private val resources: ResourceProvider = mock()
    private val testMediaModel = ProductTestUtils.generateProductMedia(REMOTE_PRODUCT_ID, REMOTE_SITE_ID)
    private val testMediaModelError = ProductTestUtils.generateMediaUploadErrorModel()
    private lateinit var mediaFileUploadHandler: MediaFileUploadHandler

    @Before
    fun setup() {
        mediaFileUploadHandler = spy(MediaFileUploadHandler(resources))
    }

    @Test
    fun `Handles product image upload error correctly`() {
        assertThat(mediaFileUploadHandler.getMediaUploadErrorCount(testMediaModel.postId)).isEqualTo(0)

        mediaFileUploadHandler.handleMediaUploadFailure(testMediaModel, testMediaModelError)
        assertThat(mediaFileUploadHandler.getMediaUploadErrorCount(testMediaModel.postId)).isEqualTo(1)
    }
}
