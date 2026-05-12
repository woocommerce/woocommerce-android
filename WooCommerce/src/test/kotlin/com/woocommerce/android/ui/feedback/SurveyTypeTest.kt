package com.woocommerce.android.ui.feedback

import com.woocommerce.android.AppUrls
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.feedback.SurveyType.MAIN
import com.woocommerce.android.ui.feedback.SurveyType.PRODUCT
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
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

    @Test
    fun `when AI Assistant SurveyType url is requested, then it uses AI Assistant Crowdsignal URL`() {
        assertThat(SurveyType.AI_ASSISTANT.url).startsWith(AppUrls.CROWDSIGNAL_AI_ASSISTANT_SURVEY)
    }

    @Test
    fun `when AI Assistant SurveyType feedback context is requested, then it uses AI Assistant context`() {
        assertThat(SurveyType.AI_ASSISTANT.feedbackContext)
            .isEqualTo(AnalyticsTracker.VALUE_AI_ASSISTANT_FEEDBACK)
    }

    @Test
    fun `when SurveyType feedback context is requested, then it preserves existing contexts`() {
        assertThat(SurveyType.entries.associateWith { it.feedbackContext })
            .contains(
                entry(SurveyType.MAIN, AnalyticsTracker.VALUE_FEEDBACK_GENERAL_CONTEXT),
                entry(SurveyType.PRODUCT, AnalyticsTracker.VALUE_FEEDBACK_PRODUCT_M3_CONTEXT),
                entry(SurveyType.STORE_ONBOARDING, AnalyticsTracker.VALUE_FEEDBACK_STORE_SETUP_CONTEXT),
                entry(SurveyType.ADDONS, AnalyticsTracker.VALUE_PRODUCT_ADDONS_FEEDBACK),
                entry(SurveyType.ANALYTICS_HUB, AnalyticsTracker.VALUE_ANALYTICS_HUB_FEEDBACK),
                entry(SurveyType.ORDER_SHIPPING_LINES, AnalyticsTracker.VALUE_ORDER_SHIPPING_LINES_FEEDBACK),
                entry(SurveyType.WOO_POS_POTENTIAL_USER, AnalyticsTracker.VALUE_WOO_POS_POTENTIAL_USER_FEEDBACK),
                entry(SurveyType.WOO_POS_CURRENT_USER, AnalyticsTracker.VALUE_WOO_POS_CURRENT_USER_FEEDBACK),
            )
    }

    companion object {
        const val ANY_DIGIT_AND_NOTHING_AFTER = "\\d(?!\\S)"
    }
}
