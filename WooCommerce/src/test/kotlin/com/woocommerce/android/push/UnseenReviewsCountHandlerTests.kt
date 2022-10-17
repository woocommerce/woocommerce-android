package com.woocommerce.android.push

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.NotificationStore

// TODO add more test cases
@ExperimentalCoroutinesApi
class UnseenReviewsCountHandlerTests : BaseUnitTest() {
    private lateinit var handler: UnseenReviewsCountHandler
    private val notificationStore: NotificationStore = mock()
    private val selectedSite: SelectedSite = mock()

    fun setup(prepareMocks: () -> Unit) {
        prepareMocks()
        handler = UnseenReviewsCountHandler(
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
            notificationStore = notificationStore,
            selectedSite = selectedSite
        )
    }

    @Test
    fun `when we get a null site, then emit 0 as count of unread reviews`() = testBlocking {
        setup {
            whenever(selectedSite.observe()).thenReturn(flowOf(null))
        }

        val unseenReviewsCount = handler.observeUnseenCount().first()

        assertThat(unseenReviewsCount).isEqualTo(0)
    }
}
