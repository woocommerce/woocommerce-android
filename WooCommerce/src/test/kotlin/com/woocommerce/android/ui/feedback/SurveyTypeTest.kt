package com.woocommerce.android.ui.feedback

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SurveyTypeTest {
    @Test
    fun `SurveyType url should include platform tag for any URL`() {
        SurveyType.values().forEach {
            assertThat(it.url.contains("woo-mobile-platform=android")).isTrue()
        }
    }
}
