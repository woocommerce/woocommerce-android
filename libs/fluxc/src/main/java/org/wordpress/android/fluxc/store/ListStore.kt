package org.wordpress.android.fluxc.store

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagedList.BoundaryCallback
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ListAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.LIST_STATE_TIMEOUT
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
import org.wordpress.android.fluxc.model.list.ListItemModel
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.fluxc.model.list.PagedListFactory
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.datasource.InternalPagedListDataSource
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.persistence.dao.ListDao
import org.wordpress.android.fluxc.store.ListStore.OnListChanged.CauseOfListChange
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * This Store is responsible for managing lists and their metadata. One of the designs goals for this Store is expose
 * as little as possible to the consumers and make sure the exposed parts are immutable. This not only moves the
 * responsibility of mutation to the Store but also makes it much easier to use the exposed data.
 */
@Singleton
class ListStore @Inject internal constructor(
    private val listDao: ListDao,
    private val coroutineContext: CoroutineContext,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? ListAction ?: return

        when (actionType) {
            ListAction.FETCHED_LIST_ITEMS ->
                coroutineEngine.launch(AppLog.T.API, this, "handleFetchedListItems") {
                    handleFetchedListItems(action.payload as FetchedListItemsPayload)
                }
            ListAction.LIST_REQUIRES_REFRESH ->
                handleListRequiresRefresh(action.payload as ListDescriptorTypeIdentifier)
            ListAction.LIST_DATA_INVALIDATED ->
                handleListDataInvalidated(action.payload as ListDescriptorTypeIdentifier)
            ListAction.MARK_LISTS_OF_TYPE_NEED_REFRESH ->
                coroutineEngine.launch(AppLog.T.API, this, "handleMarkListsOfTypeNeedRefresh") {
                    handleMarkListsOfTypeNeedRefresh(action.payload as MarkListsNeedRefreshPayload)
                }
            ListAction.LIST_DATA_FAILURE ->
                handleDataFailure(action.payload as OnListDataFailure)
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, ListStore::class.java.simpleName + " onRegister")
    }

    /**
     * This is the function that'll be used to consume lists.
     *
     * @param listDescriptor Describes which list will be consumed
     * @param dataSource Describes how to take certain actions such as fetching a list for the item type [LIST_ITEM].
     * @param lifecycle The lifecycle of the client that'll be consuming this list. It's used to make sure everything
     * is cleaned up properly once the client is destroyed.
     *
     * @return A [PagedListWrapper] that provides all the necessary information to consume a list such as its data,
     * whether the first page is being fetched, whether there are any errors etc. in `LiveData` format.
     */
    fun <LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM : Any> getList(
        listDescriptor: LIST_DESCRIPTOR,
        dataSource: ListItemDataSourceInterface<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>,
        lifecycle: Lifecycle
    ): PagedListWrapper<LIST_ITEM> {
        val factory = createPagedListFactory(listDescriptor, dataSource)
        val pagedListData = createPagedListLiveData(
                listDescriptor = listDescriptor,
                dataSource = dataSource,
                pagedListFactory = factory
        )
        return PagedListWrapper(
                data = pagedListData,
                dispatcher = mDispatcher,
                listDescriptor = listDescriptor,
                lifecycle = lifecycle,
                refresh = {
                    coroutineEngine.launch(AppLog.T.API, this, "ListStore: Refreshing first page") {
                        handleFetchList(listDescriptor, loadMore = false) { offset ->
                            dataSource.fetchList(listDescriptor, offset)
                        }
                    }
                },
                invalidate = factory::invalidate,
                parentCoroutineContext = coroutineContext
        )
    }

    /**
     * A helper function that creates a [PagedList] [LiveData] for the given [LIST_DESCRIPTOR], [dataSource] and the
     * [PagedListFactory].
     */
    private fun <LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM : Any> createPagedListLiveData(
        listDescriptor: LIST_DESCRIPTOR,
        dataSource: ListItemDataSourceInterface<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>,
        pagedListFactory: PagedListFactory<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
    ): LiveData<PagedList<LIST_ITEM>> {
        val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setInitialLoadSizeHint(listDescriptor.config.initialLoadSize)
                .setPageSize(listDescriptor.config.dbPageSize)
                .build()
        val boundaryCallback = object : BoundaryCallback<LIST_ITEM>() {
            override fun onItemAtEndLoaded(itemAtEnd: LIST_ITEM) {
                // Load more items if we are near the end of list
                coroutineEngine.launch(AppLog.T.API, this, "ListStore: Loading next page") {
                    handleFetchList(listDescriptor, loadMore = true) { offset ->
                        dataSource.fetchList(listDescriptor, offset)
                    }
                }
                super.onItemAtEndLoaded(itemAtEnd)
            }
        }
        return LivePagedListBuilder<Int, LIST_ITEM>(pagedListFactory, pagedListConfig)
                .setBoundaryCallback(boundaryCallback).build()
    }

    /**
     * A helper function that creates a [PagedListFactory] for the given [LIST_DESCRIPTOR] and [dataSource].
     */
    private fun <LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM> createPagedListFactory(
        listDescriptor: LIST_DESCRIPTOR,
        dataSource: ListItemDataSourceInterface<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
    ): PagedListFactory<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM> {
        return PagedListFactory(
                createDataSource = {
                    runBlocking {
                        val remoteItemIds = listDao.getListItems(listDescriptor)
                        val isListFullyFetched = getListState(listDescriptor) == ListState.FETCHED
                        InternalPagedListDataSource(
                                listDescriptor = listDescriptor,
                                remoteItemIds = remoteItemIds.map { RemoteId(value = it.remoteItemId) },
                                isListFullyFetched = isListFullyFetched,
                                itemDataSource = dataSource
                        )
                    }
                })
    }

    /**
     * A helper function that initiates the fetch from remote for the given [ListDescriptor].
     *
     * Before fetching the list, it'll first check if this is a valid fetch depending on the list's state. Then, it'll
     * update the list's state and emit that change. Finally, it'll calculate the offset and initiate the fetch with
     * the given [fetchList] function.
     */
    private suspend fun handleFetchList(
        listDescriptor: ListDescriptor,
        loadMore: Boolean,
        fetchList: (Long) -> Unit
    ) {
        val currentState = getListState(listDescriptor)
        if (!loadMore && currentState.isFetchingFirstPage()) {
            // already fetching the first page
            return
        } else if (loadMore && !currentState.canLoadMore()) {
            // we can only load more if there is more data to be loaded
            return
        }

        val newState = if (loadMore) ListState.LOADING_MORE else ListState.FETCHING_FIRST_PAGE
        val listModel = listDao.insertOrUpdateList(listDescriptor, newState)
        handleListStateChange(listDescriptor, newState)

        val offset = if (loadMore) listDao.getListItemsCount(listModel.id) else 0L
        fetchList(offset)
    }

    /**
     * A helper function that emits the latest [ListState] for the given [ListDescriptor].
     */
    private fun handleListStateChange(listDescriptor: ListDescriptor, newState: ListState, error: ListError? = null) {
        emitChange(OnListStateChanged(listDescriptor, newState, error))
    }

    /**
     * Handles the [ListAction.FETCHED_LIST_ITEMS] action.
     *
     * Here is how it works:
     * 1. If there was an error, update the list's state and emit the change. Otherwise:
     * 2. If the first page is fetched, delete the existing [ListItemModel]s.
     * 3. Update the [ListModel]'s state depending on whether there is more data to be fetched
     * 4. Insert the [ListItemModel]s and emit the change
     *
     * See [handleFetchList] to see how items are fetched.
     */
    private suspend fun handleFetchedListItems(payload: FetchedListItemsPayload) {
        val newState = when {
            payload.isError -> ListState.ERROR
            payload.canLoadMore -> ListState.CAN_LOAD_MORE
            else -> ListState.FETCHED
        }
        val listModel = listDao.insertOrUpdateList(payload.listDescriptor, newState)

        if (!payload.isError) {
            val listItems = payload.remoteItemIds.map { remoteItemId ->
                ListItemModel(listModel.id, remoteItemId)
            }
            listDao.deleteAndInsertItems(listModel.id, !payload.loadedMore, listItems)
        }
        val causeOfChange = if (payload.isError) {
            CauseOfListChange.ERROR
        } else {
            if (payload.loadedMore) CauseOfListChange.LOADED_MORE else CauseOfListChange.FIRST_PAGE_FETCHED
        }
        emitChange(OnListChanged(listOf(payload.listDescriptor), causeOfChange, payload.error))
        handleListStateChange(payload.listDescriptor, newState, payload.error)
    }

    suspend fun saveListFetched(
        listDescriptor: ListDescriptor,
        remoteItemIds: List<Long>,
        canLoadMore: Boolean
    ) {
        val newState = if (canLoadMore) ListState.CAN_LOAD_MORE else ListState.FETCHED
        val listModel = listDao.insertOrUpdateList(listDescriptor, newState)

        val listItems = remoteItemIds.map { remoteItemId ->
            ListItemModel(listModel.id, remoteItemId)
        }
        listDao.insertItems(listItems)
        emitChange(OnListRequiresRefresh(listDescriptor.typeIdentifier))
    }

    /**
     * Handles the [ListAction.LIST_REQUIRES_REFRESH] action.
     *
     * Whenever a type of list needs to be refreshed, [OnListRequiresRefresh] event will be emitted so the listening
     * lists can refresh themselves.
     */
    private fun handleListRequiresRefresh(typeIdentifier: ListDescriptorTypeIdentifier) {
        emitChange(OnListRequiresRefresh(type = typeIdentifier))
    }

    /**
     * Handles the [ListAction.LIST_DATA_INVALIDATED] action.
     *
     * Whenever the data of a list is invalidated, [OnListDataInvalidated] event will be emitted so the listening
     * lists can invalidate their data.
     */
    private fun handleListDataInvalidated(typeIdentifier: ListDescriptorTypeIdentifier) {
        emitChange(OnListDataInvalidated(type = typeIdentifier))
    }

    /**
     * Handles the [ListAction.MARK_LISTS_OF_TYPE_NEED_REFRESH] action.
     *
     * Sets every cached list of [MarkListsNeedRefreshPayload.excludedDescriptor]'s type to
     * [ListState.NEEDS_REFRESH] — except that descriptor itself — so each list refetches the next
     * time it is consumed. No change event is emitted, so the new state is consumed lazily (via
     * [getListState]) and this cannot trigger a refetch loop.
     */
    private suspend fun handleMarkListsOfTypeNeedRefresh(payload: MarkListsNeedRefreshPayload) {
        listDao.markListsOfTypeNeedRefresh(
            typeIdentifier = payload.excludedDescriptor.typeIdentifier.value,
            excludedUniqueIdentifier = payload.excludedDescriptor.uniqueIdentifier.value
        )
    }

    private fun handleDataFailure(event: OnListDataFailure) {
        emitChange(event)
    }

    /**
     * A helper function that returns the [ListState] for the given [ListDescriptor].
     */
    suspend fun getListState(listDescriptor: ListDescriptor): ListState {
        val listModel = listDao.getList(listDescriptor)
        val currentState = listModel?.let {
            requireNotNull(ListState.entries.firstOrNull { it.value == listModel.stateDbValue }) {
                "The stateDbValue of the ListModel didn't match any of the `ListState`s. This likely happened " +
                        "because the ListState values were altered without a DB migration."
            }
        }
        val isListStateValid = currentState != null
                && (isListStateOutdated(listModel).not() || (currentState in ListState.notExpiredStates))
        return if (isListStateValid) currentState else ListState.defaultState
    }

    /**
     * A helper function that returns whether it has been more than a certain time has passed since it's `lastModified`.
     *
     * Since we keep the state in the DB, in the case of application being closed during a fetch, it'll carry
     * over to the next session. To prevent such cases, we use a timeout approach. If it has been more than a
     * certain time since the list is last updated, we should ignore the state.
     */
    private fun isListStateOutdated(listModel: ListModel): Boolean {
        listModel.lastModified?.let {
            val lastModified = DateTimeUtils.dateUTCFromIso8601(it)
            val timePassed = (Date().time - lastModified.time)
            return timePassed > LIST_STATE_TIMEOUT
        }
        // If a list is null, it means we have never fetched it before, so it can't be outdated
        return false
    }

    /**
     * The event to be emitted when there is a change to a [ListModel].
     */
    class OnListChanged(
        val listDescriptors: List<ListDescriptor>,
        val causeOfChange: CauseOfListChange,
        error: ListError?
    ) : OnChanged<ListError>() {
        enum class CauseOfListChange {
            ERROR, FIRST_PAGE_FETCHED, LOADED_MORE
        }

        init {
            this.error = error
        }
    }

    /**
     * The event to be emitted whenever there is a change to the [ListState]
     */
    class OnListStateChanged(
        val listDescriptor: ListDescriptor,
        val newState: ListState,
        error: ListError?
    ) : OnChanged<ListError>() {
        init {
            this.error = error
        }
    }

    /**
     * The event to be emitted when a list needs to be refresh for a specific [ListDescriptorTypeIdentifier].
     */
    class OnListRequiresRefresh(val type: ListDescriptorTypeIdentifier) : OnChanged<ListError>()

    /**
     * The event to be emitted when a list's data is invalidated for a specific [ListDescriptorTypeIdentifier].
     */
    class OnListDataInvalidated(val type: ListDescriptorTypeIdentifier) : OnChanged<ListError>()

    class OnListDataFailure(val type: ListDescriptorTypeIdentifier) : OnChanged<ListError>()

    /**
     * This is the payload for [ListAction.FETCHED_LIST_ITEMS].
     *
     * @property listDescriptor List descriptor will be provided when the action to fetch items will be dispatched
     * from other Stores. The same list descriptor will need to be used in this payload so [ListStore] can decide
     * which list to update.
     * @property remoteItemIds Fetched item ids
     * @property loadedMore Indicates whether the first page is fetched or we loaded more data
     * @property canLoadMore Indicates whether there is more data to be loaded from the server.
     */
    class FetchedListItemsPayload(
        val listDescriptor: ListDescriptor,
        val remoteItemIds: List<Long>,
        val loadedMore: Boolean,
        val canLoadMore: Boolean,
        error: ListError?
    ) : Payload<ListError>() {
        init {
            this.error = error
        }
    }

    /**
     * This is the payload for [ListAction.MARK_LISTS_OF_TYPE_NEED_REFRESH].
     *
     * @property excludedDescriptor All cached lists sharing this descriptor's type identifier are
     * marked [ListState.NEEDS_REFRESH], except this descriptor itself (it is assumed to be freshly
     * fetched).
     */
    class MarkListsNeedRefreshPayload(
        val excludedDescriptor: ListDescriptor
    ) : Payload<ListError>()

    class ListError(
        val type: ListErrorType,
        val message: String? = null
    ) : OnChangedError

    enum class ListErrorType {
        GENERIC_ERROR,
        PERMISSION_ERROR,
        PARSE_ERROR,
        TIMEOUT_ERROR
    }
}
