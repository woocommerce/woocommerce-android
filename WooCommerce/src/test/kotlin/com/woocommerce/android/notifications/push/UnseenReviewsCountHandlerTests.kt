package com.woocommerce.android.notifications.push

import com.woocommerce.android.notifications.UnseenReviewsCountHandler
import com.woocommerce.android.tools.SelectedSite
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
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.store.NotificationStore

// TODO add more test cases
@ExperimentalCoroutinesApi
class UnseenReviewsCountHandlerTests : BaseUnitTest() {
    private val selectedSiteFlow = MutableStateFlow(SiteModel())

    private lateinit var handler: UnseenReviewsCountHandler
    private val notificationStore: NotificationStore = mock()
    private val selectedSite: SelectedSite = mock {
        on { observe() }.thenReturn(selectedSiteFlow)
    }

    fun setup(prepareMocks: () -> Unit = {}) {
        prepareMocks()
        handler = UnseenReviewsCountHandler(
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
            notificationStore = notificationStore,
            selectedSite = selectedSite
        )
    }

    @Test
    fun `when observing, then emit count of unread reviews`() = testBlocking {
        setup {
            val notifications = List(5) { NotificationModel(read = false) }
            whenever(notificationStore.observeNotificationsForSite(any(), anyOrNull(), anyOrNull()))
                .thenReturn(flowOf(notifications))
        }

        val unseenReviewsCount = handler.observeUnseenCount().first()

        assertThat(unseenReviewsCount).isEqualTo(5)
    }
}
