package com.woocommerce.android.ui.searchfilter

import androidx.lifecycle.Observer
import com.woocommerce.android.ui.searchfilter.SearchFilterEvent.ItemSelected
import com.woocommerce.android.ui.searchfilter.SearchFilterViewState.Empty
import com.woocommerce.android.ui.searchfilter.SearchFilterViewState.Loaded
import com.woocommerce.android.ui.searchfilter.SearchFilterViewState.Search
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class SearchFilterViewModelTest : BaseUnitTest() {

    private val viewStateObserver: Observer<SearchFilterViewState> = mock()

    private val eventObserver: Observer<SearchFilterEvent> = mock()

    private val searchFilterViewModel = SearchFilterViewModel()

    @Before
    fun setup() {
        searchFilterViewModel.viewStateLiveData.observeForever(viewStateObserver)
        searchFilterViewModel.eventLiveData.observeForever(eventObserver)
    }

    @Test
    fun `Should emit Loaded view state when start is called`() {
        val title = "title"
        val searchFilterItems = arrayOf(SearchFilterItem("name", "value"))
        val searchHint = "hint"
        searchFilterViewModel.start(title, searchFilterItems, searchHint, "requestKey")
        verify(viewStateObserver, times(1)).onChanged(
            Loaded(
                title = title,
                searchFilterItems = searchFilterItems.toList(),
                searchHint = searchHint
            )
        )
    }

    @Test
    fun `Should emit Search view state when onSearch is called and there are results for the query`() {
        val resultSearchFilterItemName = "name1"
        val resultSearchFilterItem = SearchFilterItem(resultSearchFilterItemName, "value1")
        val searchFilterItems = arrayOf(
            resultSearchFilterItem,
            SearchFilterItem("name2", "value2"),
            SearchFilterItem("name3", "value3")
        )
        searchFilterViewModel.start("title", searchFilterItems, "hint", "requestKey")
        searchFilterViewModel.onSearch(resultSearchFilterItemName)
        verify(viewStateObserver, times(1)).onChanged(
            Search(
                searchFilterItems = listOf(resultSearchFilterItem)
            )
        )
    }

    @Test
    fun `Should emit Empty view state when onSearch is called and there are NO results for the query`() {
        val query = "query"
        searchFilterViewModel.start("title", emptyArray(), "hint", "requestKey")
        searchFilterViewModel.onSearch(query)
        verify(viewStateObserver, times(1)).onChanged(Empty(query))
    }

    @Test
    fun `Should emit ItemSelected event when onItemSelected is called`() {
        val selectedItem = SearchFilterItem("name", "value")
        val requestKey = "requestKey"
        searchFilterViewModel.start("title", emptyArray(), "hint", requestKey)
        searchFilterViewModel.onItemSelected(selectedItem)
        verify(eventObserver, times(1)).onChanged(ItemSelected(selectedItem.value, requestKey))
    }
}
