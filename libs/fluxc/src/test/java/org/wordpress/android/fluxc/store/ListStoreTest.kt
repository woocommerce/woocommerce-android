package org.wordpress.android.fluxc.store

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TEST_SCOPE
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.list.ListConfig
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
import org.wordpress.android.fluxc.model.list.ListDescriptorUniqueIdentifier
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload
import org.wordpress.android.fluxc.store.ListStore.ListError
import org.wordpress.android.fluxc.store.ListStore.ListErrorType
import org.wordpress.android.fluxc.store.Store.OnChanged
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
@Suppress("UnitTestNamingRule")
class ListStoreTest {
    @get:Rule
    val databaseRule = WPDatabaseTestRule(InstrumentationRegistry.getInstrumentation().context)

    private val dispatcher = spy(Dispatcher())
    private lateinit var store: ListStore

    private val testListDescriptor = object : ListDescriptor {
        override val uniqueIdentifier = ListDescriptorUniqueIdentifier(100)
        override val typeIdentifier = ListDescriptorTypeIdentifier(200)
        override val config = ListConfig.default
    }

    private val otherListDescriptor = object : ListDescriptor {
        override val uniqueIdentifier = ListDescriptorUniqueIdentifier(300)
        override val typeIdentifier = ListDescriptorTypeIdentifier(400)
        override val config = ListConfig.default
    }

    // Shares its type identifier with [testListDescriptor] but has a different unique identifier.
    private val siblingListDescriptor = object : ListDescriptor {
        override val uniqueIdentifier = ListDescriptorUniqueIdentifier(101)
        override val typeIdentifier = ListDescriptorTypeIdentifier(200)
        override val config = ListConfig.default
    }

    @Before
    fun setUp() {
        store = ListStore(
            listDao = databaseRule.db.listDao(),
            coroutineContext = TEST_SCOPE.coroutineContext,
            coroutineEngine = initCoroutineEngine(),
            dispatcher = dispatcher
        )
    }

    // region getListState
    @Test
    fun `given no list exists, when get list state, then returns default state`() = runTest {
        val state = store.getListState(testListDescriptor)

        assertThat(state).isEqualTo(ListState.defaultState)
    }

    @Test
    fun `given list with can load more state, when get list state, then returns can load more`() = runTest {
        databaseRule.db.listDao().insertOrUpdateList(testListDescriptor, ListState.CAN_LOAD_MORE)

        val state = store.getListState(testListDescriptor)

        assertThat(state).isEqualTo(ListState.CAN_LOAD_MORE)
    }

    @Test
    fun `given list with fetched state, when get list state, then returns fetched`() = runTest {
        databaseRule.db.listDao().insertOrUpdateList(testListDescriptor, ListState.FETCHED)

        val state = store.getListState(testListDescriptor)

        assertThat(state).isEqualTo(ListState.FETCHED)
    }

    @Test
    fun `given different descriptor, when get list state, then returns default state`() = runTest {
        databaseRule.db.listDao().insertOrUpdateList(testListDescriptor, ListState.CAN_LOAD_MORE)

        val state = store.getListState(otherListDescriptor)

        assertThat(state).isEqualTo(ListState.defaultState)
    }
    // endregion

    // region saveListFetched
    @Test
    fun `given can load more, when save list fetched, then state is can load more`() = runTest {
        store.saveListFetched(testListDescriptor, listOf(1L, 2L, 3L), canLoadMore = true)

        val state = store.getListState(testListDescriptor)
        assertThat(state).isEqualTo(ListState.CAN_LOAD_MORE)
    }

    @Test
    fun `given no more pages, when save list fetched, then state is fetched`() = runTest {
        store.saveListFetched(testListDescriptor, listOf(1L, 2L, 3L), canLoadMore = false)

        val state = store.getListState(testListDescriptor)
        assertThat(state).isEqualTo(ListState.FETCHED)
    }

    @Test
    fun `given remote item ids, when save list fetched, then items are persisted`() = runTest {
        store.saveListFetched(testListDescriptor, listOf(10L, 20L, 30L), canLoadMore = false)

        val listModel = databaseRule.db.listDao().getList(testListDescriptor)!!
        val items = databaseRule.db.listDao().getListItems(listModel.id)
        assertThat(items).hasSize(3)
        assertThat(items.map { it.remoteItemId }).containsExactly(10L, 20L, 30L)
    }

    @Test
    fun `given empty list, when save list fetched, then no items are persisted`() = runTest {
        store.saveListFetched(testListDescriptor, emptyList(), canLoadMore = false)

        val listModel = databaseRule.db.listDao().getList(testListDescriptor)!!
        val items = databaseRule.db.listDao().getListItems(listModel.id)
        assertThat(items).isEmpty()
    }
    // endregion

    // region onAction - FETCHED_LIST_ITEMS (first page)
    @Test
    fun `given first page fetched, when fetched list items, then items are persisted`() = runTest {
        val payload = createFetchedListItemsPayload(
            remoteItemIds = listOf(1L, 2L, 3L),
            loadedMore = false,
            canLoadMore = true
        )

        store.onAction(ListActionBuilder.newFetchedListItemsAction(payload))

        val listModel = databaseRule.db.listDao().getList(testListDescriptor)!!
        val items = databaseRule.db.listDao().getListItems(listModel.id)
        assertThat(items.map { it.remoteItemId }).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `given first page fetched with load more, when fetched list items, then state is can load more`() = runTest {
        val payload = createFetchedListItemsPayload(
            remoteItemIds = listOf(1L, 2L),
            loadedMore = false,
            canLoadMore = true
        )

        store.onAction(ListActionBuilder.newFetchedListItemsAction(payload))

        val state = store.getListState(testListDescriptor)
        assertThat(state).isEqualTo(ListState.CAN_LOAD_MORE)
    }

    @Test
    fun `given first page fetched without load more, when fetched list items, then state is fetched`() = runTest {
        val payload = createFetchedListItemsPayload(
            remoteItemIds = listOf(1L, 2L),
            loadedMore = false,
            canLoadMore = false
        )

        store.onAction(ListActionBuilder.newFetchedListItemsAction(payload))

        val state = store.getListState(testListDescriptor)
        assertThat(state).isEqualTo(ListState.FETCHED)
    }

    @Test
    fun `given existing items, when first page fetched, then old items are replaced`() = runTest {
        store.saveListFetched(testListDescriptor, listOf(10L, 20L), canLoadMore = true)
        val payload = createFetchedListItemsPayload(
            remoteItemIds = listOf(100L, 200L, 300L),
            loadedMore = false,
            canLoadMore = false
        )

        store.onAction(ListActionBuilder.newFetchedListItemsAction(payload))

        val listModel = databaseRule.db.listDao().getList(testListDescriptor)!!
        val items = databaseRule.db.listDao().getListItems(listModel.id)
        assertThat(items.map { it.remoteItemId }).containsExactly(100L, 200L, 300L)
    }
    // endregion

    // region onAction - FETCHED_LIST_ITEMS (load more)
    @Test
    fun `given load more, when fetched list items, then items are appended`() = runTest {
        store.saveListFetched(testListDescriptor, listOf(1L, 2L), canLoadMore = true)
        val payload = createFetchedListItemsPayload(
            remoteItemIds = listOf(3L, 4L),
            loadedMore = true,
            canLoadMore = false
        )

        store.onAction(ListActionBuilder.newFetchedListItemsAction(payload))

        val listModel = databaseRule.db.listDao().getList(testListDescriptor)!!
        val items = databaseRule.db.listDao().getListItems(listModel.id)
        assertThat(items.map { it.remoteItemId }).containsExactly(1L, 2L, 3L, 4L)
    }
    // endregion

    // region onAction - FETCHED_LIST_ITEMS (error)
    @Test
    fun `given error payload, when fetched list items, then state is error`() = runTest {
        val payload = createFetchedListItemsPayload(
            remoteItemIds = emptyList(),
            loadedMore = false,
            canLoadMore = false,
            error = ListError(ListErrorType.GENERIC_ERROR, "Something went wrong")
        )

        store.onAction(ListActionBuilder.newFetchedListItemsAction(payload))

        val state = store.getListState(testListDescriptor)
        assertThat(state).isEqualTo(ListState.ERROR)
    }

    @Test
    fun `given error payload, when fetched list items, then no items are persisted`() = runTest {
        val payload = createFetchedListItemsPayload(
            remoteItemIds = listOf(1L, 2L),
            loadedMore = false,
            canLoadMore = false,
            error = ListError(ListErrorType.GENERIC_ERROR, "Something went wrong")
        )

        store.onAction(ListActionBuilder.newFetchedListItemsAction(payload))

        val listModel = databaseRule.db.listDao().getList(testListDescriptor)
        val items = databaseRule.db.listDao().getListItems(listModel!!.id)
        assertThat(items).isEmpty()
    }
    // endregion

    // region onAction - LIST_REQUIRES_REFRESH
    @Test
    fun `given type identifier, when list requires refresh, then emits refresh event`() {
        val payload = testListDescriptor.typeIdentifier

        store.onAction(ListActionBuilder.newListRequiresRefreshAction(payload))

        val captor = argumentCaptor<OnChanged<*>>()
        verify(dispatcher).emitChange(captor.capture())
        assertThat(captor.firstValue).isInstanceOf(ListStore.OnListRequiresRefresh::class.java)
        val event = captor.firstValue as ListStore.OnListRequiresRefresh
        assertThat(event.type).isEqualTo(payload)
    }
    // endregion

    // region onAction - LIST_DATA_INVALIDATED
    @Test
    fun `given type identifier, when list data invalidated, then emits invalidated event`() {
        val payload = testListDescriptor.typeIdentifier

        store.onAction(ListActionBuilder.newListDataInvalidatedAction(payload))

        val captor = argumentCaptor<OnChanged<*>>()
        verify(dispatcher).emitChange(captor.capture())
        assertThat(captor.firstValue).isInstanceOf(ListStore.OnListDataInvalidated::class.java)
        val event = captor.firstValue as ListStore.OnListDataInvalidated
        assertThat(event.type).isEqualTo(payload)
    }
    // endregion

    // region onAction - LIST_DATA_FAILURE
    @Test
    fun `given failure event, when list data failure, then emits failure event`() {
        val payload = testListDescriptor.typeIdentifier

        store.onAction(ListActionBuilder.newListDataFailureAction(ListStore.OnListDataFailure(payload)))

        val captor = argumentCaptor<OnChanged<*>>()
        verify(dispatcher).emitChange(captor.capture())
        assertThat(captor.firstValue).isInstanceOf(ListStore.OnListDataFailure::class.java)
        val event = captor.firstValue as ListStore.OnListDataFailure
        assertThat(event.type).isEqualTo(payload)
    }
    // endregion

    // region onAction - MARK_LISTS_OF_TYPE_NEED_REFRESH
    @Test
    fun `given sibling lists of same type, when mark lists of type need refresh, then siblings need refresh`() = runTest {
        store.saveListFetched(testListDescriptor, listOf(1L), canLoadMore = false)
        store.saveListFetched(siblingListDescriptor, listOf(2L), canLoadMore = false)

        store.onAction(
            ListActionBuilder.newMarkListsOfTypeNeedRefreshAction(
                ListStore.MarkListsNeedRefreshPayload(excludedDescriptor = testListDescriptor)
            )
        )

        // The excluded (just-fetched) list keeps its state; the sibling is marked for refresh.
        assertThat(store.getListState(testListDescriptor)).isEqualTo(ListState.FETCHED)
        assertThat(store.getListState(siblingListDescriptor)).isEqualTo(ListState.NEEDS_REFRESH)
    }

    @Test
    fun `given lists of a different type, when mark lists of type need refresh, then other type is unaffected`() =
        runTest {
            store.saveListFetched(testListDescriptor, listOf(1L), canLoadMore = false) // type 200
            store.saveListFetched(otherListDescriptor, listOf(2L), canLoadMore = false) // type 400

            // Exclude a (non-existent) type-200 sibling so every type-200 list is marked.
            store.onAction(
                ListActionBuilder.newMarkListsOfTypeNeedRefreshAction(
                    ListStore.MarkListsNeedRefreshPayload(excludedDescriptor = siblingListDescriptor)
                )
            )

            assertThat(store.getListState(testListDescriptor)).isEqualTo(ListState.NEEDS_REFRESH)
            assertThat(store.getListState(otherListDescriptor)).isEqualTo(ListState.FETCHED)
        }
    // endregion

    /* HELPER */

    private fun createFetchedListItemsPayload(
        remoteItemIds: List<Long>,
        loadedMore: Boolean,
        canLoadMore: Boolean,
        error: ListError? = null
    ) = FetchedListItemsPayload(
        listDescriptor = testListDescriptor,
        remoteItemIds = remoteItemIds,
        loadedMore = loadedMore,
        canLoadMore = canLoadMore,
        error = error
    )
}
