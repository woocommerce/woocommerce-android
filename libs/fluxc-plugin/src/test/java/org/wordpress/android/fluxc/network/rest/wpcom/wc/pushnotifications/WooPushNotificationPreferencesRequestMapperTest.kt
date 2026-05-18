package org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreOrderPreferences
import org.wordpress.android.fluxc.model.pushnotifications.WooPushNotificationPreferences.StoreReviewPreferences
import java.math.BigDecimal

class WooPushNotificationPreferencesRequestMapperTest {
    @Test
    fun `given all orders preference, when mapped to request body, then min amount is cleared`() {
        val preferences = WooPushNotificationPreferences(
            storeOrder = StoreOrderPreferences(enabled = true, minAmount = null)
        )

        val requestJson = Gson().toJson(preferences.toRequestMap())

        assertThat(requestJson).isEqualTo("""{"store_order":{"enabled":true,"min_amount":null}}""")
    }

    @Test
    fun `given high value orders preference, when mapped to request body, then min amount is sent`() {
        val preferences = WooPushNotificationPreferences(
            storeOrder = StoreOrderPreferences(enabled = true, minAmount = BigDecimal("100.50"))
        )

        val requestJson = Gson().toJson(preferences.toRequestMap())

        assertThat(requestJson).isEqualTo("""{"store_order":{"enabled":true,"min_amount":100.50}}""")
    }

    @Test
    fun `given empty store order preferences, when mapped to request body, then store order is omitted`() {
        val preferences = WooPushNotificationPreferences(
            storeOrder = StoreOrderPreferences()
        )

        val requestJson = Gson().toJson(preferences.toRequestMap())

        assertThat(requestJson).isEqualTo("{}")
    }

    @Test
    fun `given all reviews preference, when mapped to request body, then max rating is cleared`() {
        val preferences = WooPushNotificationPreferences(
            storeReview = StoreReviewPreferences(enabled = true, maxRating = null)
        )

        val requestJson = Gson().toJson(preferences.toRequestMap())

        assertThat(requestJson).isEqualTo("""{"store_review":{"enabled":true,"max_rating":null}}""")
    }

    @Test
    fun `given rating filtered reviews preference, when mapped to request body, then max rating is sent`() {
        val preferences = WooPushNotificationPreferences(
            storeReview = StoreReviewPreferences(enabled = true, maxRating = 3)
        )

        val requestJson = Gson().toJson(preferences.toRequestMap())

        assertThat(requestJson).isEqualTo("""{"store_review":{"enabled":true,"max_rating":3}}""")
    }
}
