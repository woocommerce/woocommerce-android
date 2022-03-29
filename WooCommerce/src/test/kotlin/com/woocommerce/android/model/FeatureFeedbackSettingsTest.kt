package com.woocommerce.android.model

import com.woocommerce.android.model.FeatureFeedbackSettings.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FeatureFeedbackSettingsTest {
    private lateinit var sut: FeatureFeedbackSettings

    @Test
    fun `when settings is created with no define state, then set it as UNANSWERED by default`() {
        sut = FeatureFeedbackSettings(
            feature = Feature.SimplePayments("test-view-name")
        )

        assertThat(sut.state).isEqualTo(FeedbackState.UNANSWERED)
    }

    @Test
    fun `when settings is created, then set tag value as requesting view with Feature class simple name`() {
        val requestingViewName = "testViewName"
        val feature = Feature.ProductVariations(requestingViewName)

        sut = FeatureFeedbackSettings(feature = feature)

        val expectedTag = requestingViewName + "_" + feature::class.java.simpleName
        assertThat(sut.feature.tag).isEqualTo(expectedTag)
    }


}
