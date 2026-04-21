package org.wordpress.android.fluxc.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel.WhatsNewAnnouncementFeature

class WhatsNewMapperTest {
    @Test
    fun `when announcement has features with null titles, then they are filtered out`() {
        val model = WhatsNewAnnouncementModel(
            announcementVersion = 1,
            minimumAppVersion = "1.0",
            maximumAppVersion = "2.0",
            appVersionTargets = emptyList(),
            detailsUrl = "https://woocommerce.com/mobile/pos/learn-more",
            isLocalized = true,
            features = listOf(
                WhatsNewAnnouncementFeature(
                    title = "Valid Feature",
                    subtitle = "Subtitle",
                    iconUrl = "",
                    iconBase64 = ""
                ),
                WhatsNewAnnouncementFeature(
                    title = null,
                    subtitle = "Should be filtered out",
                    iconUrl = "",
                    iconBase64 = ""
                ),
                WhatsNewAnnouncementFeature(
                    title = "Another Valid Feature",
                    subtitle = "Another Subtitle",
                    iconUrl = "",
                    iconBase64 = ""
                )
            )
        )

        val result = WhatsNewMapper.fromDomainModel(model)

        assertThat(result.features).hasSize(2).allMatch {
            it.title == "Valid Feature" || it.title == "Another Valid Feature"
        }
    }
}
