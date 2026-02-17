package com.woocommerce.android.ui.pushnotifications.connection

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.pushnotifications.connection.WooPushNotificationsConnectionStepsViewModel.StepState
import com.woocommerce.android.ui.pushnotifications.connection.WooPushNotificationsConnectionStepsViewModel.StepType
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPushNotificationsConnectionStepsViewModelTest : BaseUnitTest() {
    private val site = SiteModel().apply { name = "coffeebeans.com" }

    private lateinit var viewModel: WooPushNotificationsConnectionStepsViewModel

    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }

    private fun setup(prepareMocks: () -> Unit = {}) {
        prepareMocks()
        viewModel = WooPushNotificationsConnectionStepsViewModel(
            selectedSite = selectedSite,
            savedStateHandle = SavedStateHandle()
        )
    }

    @Test
    fun `when initialized, then first step is Ongoing and others are Idle`() {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.steps).hasSize(3)
        assertThat(state.steps[0].type).isEqualTo(StepType.ConnectStore)
        assertThat(state.steps[0].state).isEqualTo(StepState.Ongoing)
        assertThat(state.steps[1].type).isEqualTo(StepType.CheckPluginCompatibility)
        assertThat(state.steps[1].state).isEqualTo(StepState.Idle)
        assertThat(state.steps[2].type).isEqualTo(StepType.EnablePushNotifications)
        assertThat(state.steps[2].state).isEqualTo(StepState.Idle)
    }

    @Test
    fun `when initialized, then isDone is false`() {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.isDone).isFalse()
    }

    @Test
    fun `when initialized, then site address is set`() {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.siteAddress).isEqualTo("coffeebeans.com")
    }

    @Test
    fun `when close is clicked, then Exit event is triggered`() {
        setup()

        viewModel.onCloseClick()

        val event = viewModel.event.value
        assertThat(event).isEqualTo(Exit)
    }
}
