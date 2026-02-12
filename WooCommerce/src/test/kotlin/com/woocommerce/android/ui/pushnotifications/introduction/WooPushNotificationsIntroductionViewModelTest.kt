package com.woocommerce.android.ui.pushnotifications.introduction

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPushNotificationsIntroductionViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: WooPushNotificationsIntroductionViewModel

    private fun setup() {
        viewModel = WooPushNotificationsIntroductionViewModel(
            savedStateHandle = SavedStateHandle()
        )
    }

    @Test
    fun `when not now is clicked, then Exit event is triggered`() {
        setup()

        viewModel.onNotNowClick()

        val event = viewModel.event.value
        assertThat(event).isEqualTo(Exit)
    }

    @Test
    fun `when What is WordPress_com is clicked, then OpenUrlEvent is triggered`() {
        setup()

        viewModel.onWhatIsWPComClick()

        val event = viewModel.event.value
        assertThat(event).isInstanceOf(WooPushNotificationsIntroductionViewModel.OpenUrlEvent::class.java)
        assertThat((event as WooPushNotificationsIntroductionViewModel.OpenUrlEvent).url)
            .isEqualTo(AppUrls.LOGIN_WITH_EMAIL_WHAT_IS_WORDPRESS_COM_ACCOUNT)
    }
}
