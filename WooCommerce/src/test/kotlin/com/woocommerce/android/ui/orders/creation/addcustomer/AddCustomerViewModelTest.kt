package com.woocommerce.android.ui.orders.creation.addcustomer

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.TestDispatcher
import com.woocommerce.android.viewmodel.test
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.ListStore.ListError
import org.wordpress.android.fluxc.store.ListStore.ListErrorType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@InternalCoroutinesApi
class AddCustomerViewModelTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val listItemDataSource: AddCustomerListItemDataSource = mock()
    private val coroutineDispatchers = CoroutineDispatchers(
        TestDispatcher,
        TestDispatcher,
        TestDispatcher
    )
    private val savedStateArgs: SavedStateWithArgs = mock()

    private lateinit var viewModel: AddCustomerViewModel
    private val listStore: ListStore = mock()
    private val pagedListWrapper: PagedListWrapper<CustomerListItemType> = mock()

    private val site = SiteModel().apply { id = 1 }

    @Before
    fun setup() = test {
        whenever(pagedListWrapper.listError).thenReturn(mock())
        whenever(pagedListWrapper.isEmpty).thenReturn(mock())
        whenever(pagedListWrapper.isFetchingFirstPage).thenReturn(mock())
        whenever(pagedListWrapper.isLoadingMore).thenReturn(mock())
        whenever(pagedListWrapper.data).thenReturn(mock())
        whenever(
            listStore.getList<AddCustomerListDescriptor, AddCustomerListItemDataSource, CustomerListItemType>(
                listDescriptor = any(),
                dataSource = any(),
                lifecycle = any()
            )
        ).thenReturn(pagedListWrapper)
        whenever(selectedSite.get()).thenReturn(site)
    }

    private fun initViewModel() {
        viewModel = AddCustomerViewModel(
            savedState = savedStateArgs,
            coroutineDispatchers = coroutineDispatchers,
            listStore = listStore,
            selectedSite = selectedSite,
            listItemDataSource = listItemDataSource
        )
    }

    @Test
    fun `when view model init then fetches first page`() {
        // when
        initViewModel()

        // then
        verify(pagedListWrapper).fetchFirstPage()
    }

    @Test
    fun `when list wrapper fetches first page then true pushed to exposed live data`() {
        // given
        val isFetchingFirstPage = MutableLiveData<Boolean>()
        whenever(pagedListWrapper.isFetchingFirstPage).thenReturn(isFetchingFirstPage)
        initViewModel()

        // when
        isFetchingFirstPage.value = true

        // then
        viewModel.isFetchingFirstPage.observeForTesting {
            assertTrue(viewModel.isFetchingFirstPage.value!!)
        }
    }

    @Test
    fun `when list wrapper not fetches first page then false pushed to exposed live data`() {
        // given
        val isFetchingFirstPage = MutableLiveData<Boolean>()
        whenever(pagedListWrapper.isFetchingFirstPage).thenReturn(isFetchingFirstPage)
        initViewModel()

        // when
        isFetchingFirstPage.value = false

        // then
        viewModel.isFetchingFirstPage.observeForTesting {
            assertFalse(viewModel.isFetchingFirstPage.value!!)
        }
    }

    @Test
    fun `when list wrapper loading more then true pushed to exposed live data`() {
        // given
        val isLoadingMore = MutableLiveData<Boolean>()
        whenever(pagedListWrapper.isLoadingMore).thenReturn(isLoadingMore)
        initViewModel()

        // when
        isLoadingMore.value = true

        // then
        viewModel.isLoadingMore.observeForTesting {
            assertTrue(viewModel.isLoadingMore.value!!)
        }
    }

    @Test
    fun `when list wrapper not loading more then false pushed to exposed live data`() {
        // given
        val isLoadingMore = MutableLiveData<Boolean>()
        whenever(pagedListWrapper.isLoadingMore).thenReturn(isLoadingMore)
        initViewModel()

        // when
        isLoadingMore.value = false

        // then
        viewModel.isLoadingMore.observeForTesting {
            assertFalse(viewModel.isLoadingMore.value!!)
        }
    }

    @Test
    fun `when list wrapper got new data then data pushed to exposed live data`() {
        // given
        val list: PagedCustomersList = mock()
        val pagedListData = MutableLiveData<PagedCustomersList>()
        whenever(pagedListWrapper.data).thenReturn(pagedListData)
        initViewModel()

        // when
        pagedListData.value = list

        // then
        viewModel.pagedListData.observeForTesting {
            assertEquals(viewModel.pagedListData.value!!, list)
        }
    }

    @Test
    fun `when list data is empty and error happened then empty view has network error`() {
        // given
        val error = MutableLiveData<ListError>()
        whenever(pagedListWrapper.listError).thenReturn(error)
        initViewModel()

        // when
        error.value = ListError(ListErrorType.GENERIC_ERROR)

        // then
        viewModel.emptyViewType.observeForTesting {
            assertEquals(viewModel.emptyViewType.value!!, EmptyViewType.NETWORK_ERROR)
        }
    }

    @Test
    fun `when list data is empty and first first page then empty view has list loading`() {
        // given
        val isFetchingFirstPage = MutableLiveData<Boolean>()
        whenever(pagedListWrapper.isFetchingFirstPage).thenReturn(isFetchingFirstPage)
        initViewModel()

        // when
        isFetchingFirstPage.value = true

        // then
        viewModel.emptyViewType.observeForTesting {
            assertEquals(viewModel.emptyViewType.value!!, EmptyViewType.CUSTOMER_LIST_LOADING)
        }
    }

    @Test
    fun `when list data is empty and not loading data and with data and not error then empty view empty state`() {
        // given
        val isFetchingFirstPage = MutableLiveData<Boolean>()
        whenever(pagedListWrapper.isFetchingFirstPage).thenReturn(isFetchingFirstPage)
        val list: PagedCustomersList = mock()
        val pagedListData = MutableLiveData<PagedCustomersList>(list)
        whenever(pagedListWrapper.data).thenReturn(pagedListData)
        initViewModel()

        // when
        isFetchingFirstPage.value = false

        // then
        viewModel.emptyViewType.observeForTesting {
            assertEquals(viewModel.emptyViewType.value!!, EmptyViewType.CUSTOMER_LIST)
        }
    }

    @Test
    fun `in refresh then fetches first page`() {
        // when
        initViewModel()
        viewModel.onRefresh()

        // then
        verify(pagedListWrapper, times(2)).fetchFirstPage()
    }
}
