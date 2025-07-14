package com.woocommerce.android.ui.feedback

import com.woocommerce.android.BuildConfig
import com.woocommerce.android.ui.feedback.SurveyType.MAIN
import com.woocommerce.android.ui.feedback.SurveyType.PRODUCT
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SurveyTypeTest {
    @Test
    fun `SurveyType url should include platform tag for any URL`() {
        assertThat(SurveyType.entries.toTypedArray()).allSatisfy {
            assertThat(it.url.contains("woo-mobile-platform=android")).isTrue()
        }
    }

    @Test
    fun `Product SurveyType url should include a milestone tag`() {
        assertThat(PRODUCT.url.contains(Regex("product-milestone=$ANY_DIGIT_AND_NOTHING_AFTER"))).isTrue()
    }

    @Test
    fun `Main SurveyType url should NOT include a milestone tag`() {
        assertThat(MAIN.url.contains(Regex("milestone=$ANY_DIGIT_AND_NOTHING_AFTER"))).isFalse()
    }

    @Test
    fun `SurveyType url should include app version form tag for any URL`() {
        assertThat(SurveyType.entries.toTypedArray()).allSatisfy {
            assertThat(it.url.contains("app-version=${BuildConfig.VERSION_NAME}")).isTrue()
        }
    }

    companion object {
        const val ANY_DIGIT_AND_NOTHING_AFTER = "\\d(?!\\S)"
    }
}
