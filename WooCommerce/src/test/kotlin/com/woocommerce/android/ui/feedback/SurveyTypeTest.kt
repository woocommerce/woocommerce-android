package com.woocommerce.android.ui.feedback

import com.woocommerce.android.BuildConfig
import com.woocommerce.android.ui.feedback.SurveyType.MAIN
import com.woocommerce.android.ui.feedback.SurveyType.PRODUCT
import com.woocommerce.android.ui.feedback.SurveyType.SHIPPING_LABELS
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SurveyTypeTest {
    @Test
    fun `SurveyType url should include platform tag for any URL`() {
        SurveyType.values().forEach {
            assertThat(it.url.contains("woo-mobile-platform=android")).isTrue()
        }
    }

    @Test
    fun `Product SurveyType url should include a milestone tag`() {
        assertThat(PRODUCT.url.contains(Regex("product-milestone=${anyDigitAndNothingAfter}"))).isTrue()
    }

    @Test
    fun `ShippingLabels SurveyType url should include a milestone tag`() {
        assertThat(SHIPPING_LABELS.url.contains(Regex("shipping_label_milestone=${anyDigitAndNothingAfter}"))).isTrue()
    }

    @Test
    fun `Main SurveyType url should NOT include a milestone tag`() {
        assertThat(MAIN.url.contains(Regex("milestone=${anyDigitAndNothingAfter}"))).isFalse()
    }

    @Test
    fun `Main SurveyType url should include an app version`() {
        assertThat(MAIN.url.contains(Regex("app-version=${BuildConfig.VERSION_NAME}"))).isTrue()
    }

    @Test
    fun `ShippingLabels SurveyType url should NOT include an app version`() {
        assertThat(SHIPPING_LABELS.url.contains(Regex("app-version=${BuildConfig.VERSION_NAME}"))).isFalse()
    }

    @Test
    fun `Product SurveyType url should NOT include an app version`() {
        assertThat(PRODUCT.url.contains(Regex("app-version=${BuildConfig.VERSION_NAME}"))).isFalse()
    }

    companion object {
        const val anyDigitAndNothingAfter = "\\d(?!\\S)"
    }
}
