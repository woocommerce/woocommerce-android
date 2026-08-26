package com.woocommerce.android.notifications.push

import com.woocommerce.android.notifications.UnseenReviewsCountHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.domain.SupportsReviewsReadStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.store.WpComPushNotificationStore

// TODO add more test cases
@ExperimentalCoroutinesApi
class UnseenReviewsCountHandlerTests : BaseUnitTest() {
    private val selectedSiteFlow = MutableStateFlow(SiteModel())

    private lateinit var handler: UnseenReviewsCountHandler
    private val wpComPushNotificationStore: WpComPushNotificationStore = mock()
    private val supportsReviewsReadStatus: SupportsReviewsReadStatus = mock()
    private val selectedSite: SelectedSite = mock {
        on { observe() }.thenReturn(selectedSiteFlow)
    }

    private suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        handler = UnseenReviewsCountHandler(
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
            wpComPushNotificationStore = wpComPushNotificationStore,
            supportsReviewsReadStatus = supportsReviewsReadStatus,
            selectedSite = selectedSite
        )
    }

    @Test
    fun `when observing, then emit count of unread reviews`() = testBlocking {
        setup {
            // GIVEN
            whenever(supportsReviewsReadStatus()).thenReturn(true)
            val notifications = List(5) { NotificationModel(read = false) }
            whenever(wpComPushNotificationStore.observeNotificationsForSite(any(), anyOrNull(), anyOrNull()))
                .thenReturn(flowOf(notifications))
        }

        // WHEN
        val unseenReviewsCount = handler.observeUnseenCount().first()

        // THEN
        assertThat(unseenReviewsCount).isEqualTo(5)
    }

    @Test
    fun `given read status is unsupported, when observing, then emit zero`() = testBlocking {
        setup {
            // GIVEN
            whenever(supportsReviewsReadStatus()).thenReturn(false)
        }

        // WHEN
        val unseenReviewsCount = handler.observeUnseenCount().first()

        // THEN
        assertThat(unseenReviewsCount).isEqualTo(0)
        verify(wpComPushNotificationStore, never()).observeNotificationsForSite(any(), anyOrNull(), anyOrNull())
    }
}
