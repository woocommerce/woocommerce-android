package com.woocommerce.android.model

import com.woocommerce.android.model.FeatureFeedbackSettings.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FeatureFeedbackSettingsTest {
    private lateinit var sut: FeatureFeedbackSettings

    @Test
    fun `when settings is created with no define state, then set it as UNANSWERED by default`() {
        sut = FeatureFeedbackSettings(Feature.values().first())

        assertThat(sut.feedbackState).isEqualTo(FeedbackState.UNANSWERED)
    }

    @Test
    fun `when settings is created, then set tag value as requesting view with Feature class simple name`() {
        val selectedFeature = Feature.values().first()

        sut = FeatureFeedbackSettings(selectedFeature)

        assertThat(sut.key).isEqualTo(selectedFeature.toString())
    }
}
