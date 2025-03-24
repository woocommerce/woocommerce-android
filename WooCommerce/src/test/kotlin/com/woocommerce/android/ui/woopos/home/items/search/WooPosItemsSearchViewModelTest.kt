package com.woocommerce.android.ui.woopos.home.items.search

import app.cash.turbine.test
import com.woocommerce.android.ui.woopos.home.items.WooPosItem
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosItemsSearchViewModelTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @Test
    fun `given less than max popular items and recent searches, when view model created, then all items are shown`() =
        runTest {
            // GIVEN
            val mockEmptyStateProvider: WooPosItemsSearchEmptyStateProvider = mock()

            val popularItems = listOf(
                WooPosItem.Product.Simple(id = 1, name = "Popular Item 1", price = "$10.0", imageUrl = null),
                WooPosItem.Product.Simple(id = 2, name = "Popular Item 2", price = "$15.0", imageUrl = null)
            )
            val recentSearches = listOf(
                "Recent Search 1"
            )

            whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(popularItems)
            whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(recentSearches)

            // WHEN
            val viewModel = WooPosItemsSearchViewModel(mockEmptyStateProvider)

            // THEN
            viewModel.viewState.test {
                val value = awaitItem()
                assertThat(value).isInstanceOf(WooPosItemsSearchViewState.EmptySearchQuery::class.java)

                val emptySearchQuery = value as WooPosItemsSearchViewState.EmptySearchQuery
                assertThat(emptySearchQuery.popularItems).hasSize(2)
                assertThat(emptySearchQuery.recentSearches).hasSize(1)
            }
        }

    @Test
    fun `given empty popular items and recent searches, when view model created, then empty lists are shown`() =
        runTest {
            // GIVEN
            val mockEmptyStateProvider: WooPosItemsSearchEmptyStateProvider = mock()
            whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(emptyList())
            whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(emptyList())

            // WHEN
            val viewModel = WooPosItemsSearchViewModel(mockEmptyStateProvider)

            // THEN
            viewModel.viewState.test {
                val value = awaitItem()
                assertThat(value).isInstanceOf(WooPosItemsSearchViewState.EmptySearchQuery::class.java)

                val emptySearchQuery = value as WooPosItemsSearchViewState.EmptySearchQuery
                assertThat(emptySearchQuery.popularItems).isEmpty()
                assertThat(emptySearchQuery.recentSearches).isEmpty()
            }
        }

    @Test
    fun `when view model created, then initial state is Empty`() = runTest {
        // GIVEN
        val mockEmptyStateProvider: WooPosItemsSearchEmptyStateProvider = mock()
        whenever(mockEmptyStateProvider.getPopularItems()).thenAnswer {
            emptyList<WooPosItem>()
        }
        whenever(mockEmptyStateProvider.getLastSearches()).thenAnswer {
            emptyList<String>()
        }

        // WHEN
        val viewModel = WooPosItemsSearchViewModel(mockEmptyStateProvider)

        // THEN
        assertThat(viewModel.viewState.value).isEqualTo(WooPosItemsSearchViewState.Empty)
    }

    @Test
    fun `given more than max items count, when view model created, then only max items are shown`() = runTest {
        // GIVEN
        val mockEmptyStateProvider: WooPosItemsSearchEmptyStateProvider = mock()
        val popularItems = listOf(
            WooPosItem.Product.Simple(id = 1, name = "Popular Item 1", price = "$10.0", imageUrl = null),
            WooPosItem.Product.Simple(id = 2, name = "Popular Item 2", price = "$15.0", imageUrl = null),
            WooPosItem.Product.Simple(id = 3, name = "Popular Item 3", price = "$20.0", imageUrl = null),
            WooPosItem.Product.Simple(id = 4, name = "Popular Item 4", price = "$25.0", imageUrl = null)
        )
        val recentSearches = listOf(
            "Recent Search 1",
            "Recent Search 2",
            "Recent Search 3",
            "Recent Search 4"
        )
        whenever(mockEmptyStateProvider.getPopularItems()).thenReturn(popularItems)
        whenever(mockEmptyStateProvider.getLastSearches()).thenReturn(recentSearches)

        // WHEN
        val viewModel = WooPosItemsSearchViewModel(mockEmptyStateProvider)

        // THEN
        viewModel.viewState.test {
            val value = awaitItem()
            assertThat(value).isInstanceOf(WooPosItemsSearchViewState.EmptySearchQuery::class.java)
            val emptySearchQuery = value as WooPosItemsSearchViewState.EmptySearchQuery
            assertThat(emptySearchQuery.popularItems).hasSize(3)
            assertThat(emptySearchQuery.popularItems.map { it.id }).containsExactly(
                1,
                2,
                3
            ) // Should be the first 3 items
            assertThat(emptySearchQuery.recentSearches).hasSize(3)
            assertThat(emptySearchQuery.recentSearches).containsExactly(
                "Recent Search 1",
                "Recent Search 2",
                "Recent Search 3"
            )
        }
    }
}
