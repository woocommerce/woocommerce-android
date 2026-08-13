package com.woocommerce.android.network

import app.cash.turbine.test
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WPComSiteInvalidationEvent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WPComSiteInvalidationReason

@OptIn(ExperimentalCoroutinesApi::class)
class WPComSiteInvalidationNotifierTest : BaseUnitTest() {
    private val sut = WPComSiteInvalidationNotifier()

    @Test
    fun `when site is invalidated, then the event is emitted`() = testBlocking {
        val event = WPComSiteInvalidationEvent(SITE_ID, WPComSiteInvalidationReason.UNKNOWN_BLOG)

        sut.siteInvalidationEvents.test {
            sut.onSiteInvalidated(event)

            assertThat(awaitItem()).isEqualTo(event)
        }
    }

    @Test
    fun `when Jetpack connection is missing, then the typed event is emitted`() = testBlocking {
        val event = WPComSiteInvalidationEvent(
            SITE_ID,
            WPComSiteInvalidationReason.JETPACK_CONNECTION_MISSING
        )

        sut.siteInvalidationEvents.test {
            sut.onSiteInvalidated(event)

            assertThat(awaitItem()).isEqualTo(event)
        }
    }

    companion object {
        private const val SITE_ID = 789L
    }
}
