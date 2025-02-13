package org.wordpress.android.fluxc.wc.product

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ProductReliabilityTest {
    @Test
    fun testBooleansWrongFormatted() {
        val result = UnitTestUtils
            .getStringFromResourceFile(this.javaClass, "wc/product_reliability_booleans.json")
            ?.run { Gson().fromJson(this, ProductApiResponse::class.java) }

        assertThat(result).isNotNull
        assertThat(result?.sold_individually).isTrue
        assertThat(result?.purchasable).isTrue
    }
}
