package com.woocommerce.android.model

import com.woocommerce.android.model.FeatureFeedbackSettings.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FeatureFeedbackSettingsTest {
    private lateinit var sut: FeatureFeedbackSettings

    @Test
    fun `when settings is created with no define state, then set it as UNANSWERED by default`() {
        val featureKey = FeatureFeedbackKey("testViewName", Feature.values().first())

        sut = FeatureFeedbackSettings(featureFeedbackKey = featureKey)

        assertThat(sut.state).isEqualTo(FeedbackState.UNANSWERED)
    }

    @Test
    fun `when settings is created, then set tag value as requesting view with Feature class simple name`() {
        val requestingViewName = "testViewName"
        val selectedFeature = Feature.values().first()
        val featureKey = FeatureFeedbackKey(requestingViewName, selectedFeature)

        sut = FeatureFeedbackSettings(featureFeedbackKey = featureKey)

        val expectedTag = requestingViewName + "_" + selectedFeature.toString()
        assertThat(sut.featureFeedbackKey.value).isEqualTo(expectedTag)
    }
}
