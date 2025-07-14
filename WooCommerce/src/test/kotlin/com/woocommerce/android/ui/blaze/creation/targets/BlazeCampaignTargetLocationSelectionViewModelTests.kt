package com.woocommerce.android.ui.blaze.creation.targets

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.Location
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetLocationSelectionViewModel.TargetLocationResult
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BlazeCampaignTargetLocationSelectionViewModelTests : BaseUnitTest() {
    private val sampleLocations = listOf(
        Location(id = 0, name = "Location 0"),
        Location(id = 1, name = "Location 1"),
        Location(id = 2, name = "Location 2"),
        Location(id = 3, name = "Location 3"),
    )

    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val blazeRepository: BlazeRepository = mock {
        onBlocking { fetchLocations(any()) } doReturn Result.success(sampleLocations)
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.getArgument<Any?>(0).toString() }
    }
    private lateinit var viewModel: BlazeCampaignTargetLocationSelectionViewModel

    suspend fun setup(selectedLocations: List<Location> = emptyList(), prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = BlazeCampaignTargetLocationSelectionViewModel(
            savedStateHandle = BlazeCampaignTargetLocationSelectionFragmentArgs(
                locations = selectedLocations.toTypedArray()
            ).toSavedStateHandle(),
            resourceProvider = resourceProvider,
            blazeRepository = blazeRepository,
            analyticsTrackerWrapper = analyticsTracker
        )
    }

    @Test
    fun `given a list of locations selected, when the view model is created, then show the selected locations`() =
        testBlocking {
            val selectedLocations = listOf(sampleLocations[0], sampleLocations[1], sampleLocations[2])
            setup(selectedLocations)

            val viewState = viewModel.viewState.getOrAwaitValue()

            assertThat(viewState.selectedItems).containsExactly(
                TargetSelectionViewState.SelectionItem(selectedLocations[0].id.toString(), selectedLocations[0].name),
                TargetSelectionViewState.SelectionItem(selectedLocations[1].id.toString(), selectedLocations[1].name),
                TargetSelectionViewState.SelectionItem(selectedLocations[2].id.toString(), selectedLocations[2].name)
            )
        }

    @Test
    fun `when searching for locations, then show returned locations`() = testBlocking {
        setup()

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onSearchActiveStateChanged(true)
            viewModel.onSearchQueryChanged("query")
            advanceUntilIdle()
        }.last()

        assertThat(viewState.searchState).isEqualTo(
            TargetSelectionViewState.SearchState.Results(
                resultItems = sampleLocations.map { location ->
                    TargetSelectionViewState.SearchState.Results.SearchItem(
                        id = location.id.toString(),
                        title = location.name,
                        subtitle = null,
                        type = null
                    )
                }
            )
        )
    }

    @Test
    fun `when location search fails, then show error state`() = testBlocking {
        setup {
            whenever(blazeRepository.fetchLocations(any())) doReturn Result.failure(Exception())
        }

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onSearchActiveStateChanged(true)
            viewModel.onSearchQueryChanged("query")
            advanceUntilIdle()
        }.last()

        assertThat(viewState.searchState).isEqualTo(TargetSelectionViewState.SearchState.Error)
    }

    @Test
    fun `when location search returns no results, then show no results state`() = testBlocking {
        setup {
            whenever(blazeRepository.fetchLocations(any())) doReturn Result.success(emptyList())
        }

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onSearchActiveStateChanged(true)
            viewModel.onSearchQueryChanged("query")
            advanceUntilIdle()
        }.last()

        assertThat(viewState.searchState).isEqualTo(TargetSelectionViewState.SearchState.NoResults)
    }

    @Test
    fun `when retrying location search, then return updated results`() = testBlocking {
        setup {
            whenever(blazeRepository.fetchLocations(any())).thenReturn(Result.failure(Exception()))
                .thenReturn(Result.success(sampleLocations))
        }

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onSearchActiveStateChanged(true)
            viewModel.onSearchQueryChanged("query")
            advanceUntilIdle()
            viewModel.onRetrySearchTapped()
            advanceUntilIdle()
        }.last()

        assertThat(viewState.searchState).isEqualTo(
            TargetSelectionViewState.SearchState.Results(
                resultItems = sampleLocations.map { location ->
                    TargetSelectionViewState.SearchState.Results.SearchItem(
                        id = location.id.toString(),
                        title = location.name,
                        subtitle = null,
                        type = null
                    )
                }
            )
        )
    }

    @Test
    fun `when selecting new locations from search, then update state`() = testBlocking {
        setup()

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onSearchActiveStateChanged(true)
            viewModel.onSearchItemTapped(
                sampleLocations[0].let { location ->
                    TargetSelectionViewState.SearchState.Results.SearchItem(
                        id = location.id.toString(),
                        title = location.name,
                        subtitle = null,
                        type = null
                    )
                }
            )
        }.last()

        assertThat(viewState.selectedItems).contains(
            TargetSelectionViewState.SelectionItem(sampleLocations[0].id.toString(), sampleLocations[0].name)
        )
    }

    @Test
    fun `when toggling location, then update state`() = testBlocking {
        setup(sampleLocations)

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onItemToggled(
                TargetSelectionViewState.SelectionItem(
                    id = sampleLocations[0].id.toString(),
                    title = sampleLocations[0].name
                )
            )
        }.last()

        assertThat(viewState.selectedItems).doesNotContain(
            TargetSelectionViewState.SelectionItem(sampleLocations[0].id.toString(), sampleLocations[0].name)
        )
    }

    @Test
    fun `when saving, then return selected locations`() = testBlocking {
        setup(sampleLocations)

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onSaveTapped()
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.ExitWithResult(TargetLocationResult(sampleLocations)))
    }
}
