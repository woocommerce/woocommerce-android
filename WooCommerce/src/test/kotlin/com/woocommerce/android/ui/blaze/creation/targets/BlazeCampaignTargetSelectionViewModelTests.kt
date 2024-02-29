package com.woocommerce.android.ui.blaze.creation.targets

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.Device
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SelectionItem
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BlazeCampaignTargetSelectionViewModelTests : BaseUnitTest() {
    private val sampleDevices = listOf(
        Device(id = "0", name = "Device 0"),
        Device(id = "1", name = "Device 1"),
        Device(id = "2", name = "Device 2"),
        Device(id = "3", name = "Device 3"),
    )

    private val devicesFlow = MutableStateFlow(sampleDevices)
    private val blazeRepository: BlazeRepository = mock {
        on { observeDevices() } doReturn devicesFlow
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.getArgument<Any?>(0).toString() }
    }
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: BlazeCampaignTargetSelectionViewModel

    suspend fun setup(selectedIds: List<String> = emptyList(), prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = BlazeCampaignTargetSelectionViewModel(
            blazeRepository = blazeRepository,
            savedStateHandle = BlazeCampaignTargetSelectionFragmentArgs(
                selectedIds = selectedIds.toTypedArray(),
                targetType = BlazeTargetType.DEVICE
            ).toSavedStateHandle(),
            resourceProvider = resourceProvider,
            analyticsTrackerWrapper = analyticsTracker
        )
    }

    @Test
    fun `given a list of devices selected, when the view model is created, then show the selected devices`() =
        testBlocking {
            val selectedDevices = listOf(sampleDevices[0], sampleDevices[1], sampleDevices[2])
            setup(selectedIds = selectedDevices.map { it.id })

            val viewState = viewModel.viewState.getOrAwaitValue()

            assertThat(viewState.selectedItems).containsExactly(
                SelectionItem(id = "0", title = "Device 0"),
                SelectionItem(id = "1", title = "Device 1"),
                SelectionItem(id = "2", title = "Device 2")
            )
        }

    @Test
    fun `when observing devices, then show returned devices`() = testBlocking {
        setup()

        devicesFlow.value = sampleDevices.dropLast(1)

        val viewState = viewModel.viewState.getOrAwaitValue()

        assertThat(viewState.items).containsExactly(
            SelectionItem(id = "0", title = "Device 0"),
            SelectionItem(id = "1", title = "Device 1"),
            SelectionItem(id = "2", title = "Device 2")
        )
        assertThat(viewState.selectedItems).isEmpty()
    }

    @Test
    fun `when an item is selected, then update the selected items`() = testBlocking {
        setup()

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onItemToggled(SelectionItem(id = "0", title = "Device 0"))
        }.last()

        assertThat(viewState.selectedItems).containsExactly(viewState.items[0])
    }

    @Test
    fun `when an item is deselected, then update the selected items`() = testBlocking {
        setup(selectedIds = listOf("0", "1", "2"))

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onItemToggled(SelectionItem(id = "0", title = "Device 0"))
        }.last()

        assertThat(viewState.selectedItems).doesNotContain(SelectionItem(id = "0", title = "Device 0"))
    }

    @Test
    fun `when all items are selected, then select all items`() = testBlocking {
        setup(selectedIds = listOf("0", "1", "2"))

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onAllButtonTapped()
        }.last()

        assertThat(viewState.selectedItems).isEqualTo(sampleDevices.map { SelectionItem(it.id, it.name) })
    }

    @Test
    fun `when save is tapped, then return selected items`() = testBlocking {
        setup(selectedIds = listOf("0", "1", "2"))

        val selectedItems = viewModel.viewState.runAndCaptureValues {
            viewModel.onSaveTapped()
        }.last().selectedItems

        assertThat(selectedItems).containsExactly(
            SelectionItem(id = "0", title = "Device 0"),
            SelectionItem(id = "1", title = "Device 1"),
            SelectionItem(id = "2", title = "Device 2")
        )
    }
}
