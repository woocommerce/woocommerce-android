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

    private val targetUrl = "https://woo.com"

    fun setup(params: Map<String, String> = emptyMap()) {
        viewModel = BlazeCampaignCreationAdDestinationParametersViewModel(
            BlazeCampaignCreationAdDestinationParametersFragmentArgs(
                DestinationParameters(targetUrl, params)
            ).toSavedStateHandle()
        )
    }

    @Test
    fun `onBackPressed triggers correct event`() = testBlocking {
        // Setup
        setup()
        val expectedParameters = mapOf("key" to "value")
        viewModel.onAddParameterTapped()
        viewModel.onParameterChanged("key", "value")
        viewModel.onParameterSaved("key", "value")

        // Act
        viewModel.onBackPressed()

        // Assert
        val event = viewModel.event.getOrAwaitValue()
        assertThat(event is ExitWithResult<*>).isTrue()
        val result = (event as ExitWithResult<*>).data as DestinationParameters
        assertThat(result.parameters).isEqualTo(expectedParameters)
        assertThat(result.targetUrl).isEqualTo(targetUrl)
    }

    @Test
    fun `onAddParameterTapped updates view state correctly`() = testBlocking {
        // Setup
        val params = mapOf("paramKey" to "paramValue")
        setup(params)

        // Act
        viewModel.onAddParameterTapped()

        // Assert
        val state = viewModel.viewState.getOrAwaitValue()
        assertThat(state.bottomSheetState).isEqualTo(
            ParameterBottomSheetState.Editing(
                targetUrl = targetUrl,
                parameters = params
            )
        )
    }

    @Test
    fun `onParameterTapped updates view state correctly`() = testBlocking {
        // Setup
        val key1 = "key1"
        val value1 = "value1"
        val params = mapOf(key1 to value1)
        setup(params)
        viewModel.onAddParameterTapped()
        viewModel.onParameterChanged("key2", "someValue")
        viewModel.onParameterSaved("key2", "newValue")

        // Act
        viewModel.onParameterTapped(key1)

        // Assert
        val state = viewModel.viewState.getOrAwaitValue()
        assertThat(state.bottomSheetState is ParameterBottomSheetState.Editing).isTrue()
        assertThat((state.bottomSheetState as ParameterBottomSheetState.Editing).key).isEqualTo(key1)
        assertThat((state.bottomSheetState as ParameterBottomSheetState.Editing).value).isEqualTo(value1)
    }

    @Test
    fun `onDeleteParameterTapped updates view state correctly`() = testBlocking {
        // Setup
        val key = "key1"
        val value = "value1"
        val params = mapOf(key to value)
        setup(params)

        // Act
        viewModel.onDeleteParameterTapped(key)

        // Assert
        val state = viewModel.viewState.getOrAwaitValue()
        assertThat(state.parameters).doesNotContainKey(key)
    }

    @Test
    fun `onParameterBottomSheetDismissed updates view state correctly`() = testBlocking {
        // Setup
        setup()
        viewModel.onAddParameterTapped()

        // Act
        viewModel.onParameterBottomSheetDismissed()

        // Assert
        val state = viewModel.viewState.getOrAwaitValue()
        assertThat(state.bottomSheetState is ParameterBottomSheetState.Hidden).isTrue()
    }
}
