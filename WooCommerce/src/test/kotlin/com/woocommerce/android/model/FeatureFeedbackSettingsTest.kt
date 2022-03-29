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
        sut = FeatureFeedbackSettings(
            feature = Feature.ProductVariations("test-view-name")
        )

        assertThat(sut.feature.tag).isEqualTo("")
    }


}
