package com.woocommerce.android.ui.products

import com.woocommerce.android.tools.ProductImageMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ProductImageLoaderTest {
    private val images = mutableListOf<String?>()
    private val target = object : ProductImageTarget {
        override fun show(imageUrl: String?) {
            images.add(imageUrl)
        }
    }

    @Test
    fun `when a row is rebound during loading, then only show the new product image`() =
        runTest(UnconfinedTestDispatcher()) {
            // GIVEN
            val firstImage = CompletableDeferred<String?>()
            val secondImage = CompletableDeferred<String?>()
            val imageMap: ProductImageMap = mock {
                on { get(1) } doSuspendableAnswer { firstImage.await() }
                on { get(2) } doSuspendableAnswer { secondImage.await() }
            }
            val loader = ProductImageLoader(imageMap, target, backgroundScope)
            loader.load(1)

            // WHEN
            loader.load(2)
            secondImage.complete("second.png")
            firstImage.complete("first.png")

            // THEN
            assertThat(images).containsExactly(null, null, "second.png")
        }

    @Test
    fun `when loading is canceled, then stop updating the target until another load`() =
        runTest(UnconfinedTestDispatcher()) {
            // GIVEN
            val image = CompletableDeferred<String?>()
            val imageMap: ProductImageMap = mock {
                on { get(1) } doSuspendableAnswer { image.await() }
            }
            val loader = ProductImageLoader(imageMap, target, backgroundScope)
            loader.load(1)

            // WHEN
            loader.cancel()
            image.complete("product.png")

            // THEN
            assertThat(images).containsExactly(null)
            loader.load(1)
            assertThat(images).containsExactly(null, null, "product.png")
        }
}
