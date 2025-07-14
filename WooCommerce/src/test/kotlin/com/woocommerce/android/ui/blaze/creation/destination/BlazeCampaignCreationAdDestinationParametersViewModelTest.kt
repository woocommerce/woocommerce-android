package com.woocommerce.android.ui.blaze.creation.destination

import com.woocommerce.android.ui.blaze.BlazeRepository.DestinationParameters
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationParametersViewModel.ViewState.ParameterBottomSheetState
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlazeCampaignCreationAdDestinationParametersViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: BlazeCampaignCreationAdDestinationParametersViewModel

    private val targetUrl = "https://woocommerce.com"

    fun setup(params: Map<String, String> = emptyMap()) {
        viewModel = BlazeCampaignCreationAdDestinationParametersViewModel(
            BlazeCampaignCreationAdDestinationParametersFragmentArgs(
                DestinationParameters(targetUrl, params)
            ).toSavedStateHandle()
        )
    }

    @Test
    fun `when tapping the back button, then destination parameters are returned`() = testBlocking {
        // GIVEN
        setup()
        val expectedParameters = mapOf("key" to "value")
        viewModel.onAddParameterTapped()
        viewModel.onParameterChanged("key", "value")
        viewModel.onParameterSaved("key", "value")

        // WHEN
        viewModel.onBackPressed()

        // THEN
        val event = viewModel.event.getOrAwaitValue()
        assertThat(event is ExitWithResult<*>).isTrue()
        val result = (event as ExitWithResult<*>).data as DestinationParameters
        assertThat(result.parameters).isEqualTo(expectedParameters)
        assertThat(result.targetUrl).isEqualTo(targetUrl)
    }

    @Test
    fun `when add parameters button tapped, then bottom sheet is displayed`() = testBlocking {
        // GIVEN
        val params = mapOf("paramKey" to "paramValue")
        setup(params)

        // WHEN
        viewModel.onAddParameterTapped()

        // THEN
        val state = viewModel.viewState.getOrAwaitValue()
        assertThat(state.bottomSheetState).isEqualTo(
            ParameterBottomSheetState.Editing(
                targetUrl = targetUrl,
                parameters = params
            )
        )
    }

    @Test
    fun `when a parameter is tapped, then the parameter data is loaded`() = testBlocking {
        // GIVEN
        val key1 = "key1"
        val value1 = "value1"
        val params = mapOf(key1 to value1)
        setup(params)
        viewModel.onAddParameterTapped()
        viewModel.onParameterChanged("key2", "someValue")
        viewModel.onParameterSaved("key2", "newValue")

        // WHEN
        viewModel.onParameterTapped(key1)

        // THEN
        val state = viewModel.viewState.getOrAwaitValue()
        assertThat(state.bottomSheetState is ParameterBottomSheetState.Editing).isTrue()
        assertThat((state.bottomSheetState as ParameterBottomSheetState.Editing).key).isEqualTo(key1)
        assertThat((state.bottomSheetState as ParameterBottomSheetState.Editing).value).isEqualTo(value1)
    }

    @Test
    fun `when parameter delete button is tapped, then parameter is removed`() = testBlocking {
        // GIVEN
        val key = "key1"
        val value = "value1"
        val params = mapOf(key to value)
        setup(params)

        // WHEN
        viewModel.onDeleteParameterTapped(key)

        // THEN
        val state = viewModel.viewState.getOrAwaitValue()
        assertThat(state.parameters).doesNotContainKey(key)
    }

    @Test
    fun `when parameter bottom is dismissed, then bottom sheet is hidden`() = testBlocking {
        // GIVEN
        setup()
        viewModel.onAddParameterTapped()

        // WHEN
        viewModel.onParameterBottomSheetDismissed()

        // THEN
        val state = viewModel.viewState.getOrAwaitValue()
        assertThat(state.bottomSheetState is ParameterBottomSheetState.Hidden).isTrue()
    }
}
