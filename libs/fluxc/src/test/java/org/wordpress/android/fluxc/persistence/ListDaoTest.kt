package org.wordpress.android.fluxc.persistence

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.list.ListConfig
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
import org.wordpress.android.fluxc.model.list.ListDescriptorUniqueIdentifier
import org.wordpress.android.fluxc.model.list.ListItemModel
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.fluxc.persistence.dao.ListDao

@RunWith(RobolectricTestRunner::class)
class ListDaoTest {
    @get:Rule
    val dbRule = WPDatabaseTestRule(InstrumentationRegistry.getInstrumentation().context)

    private lateinit var dao: ListDao

    private val testDescriptor = object : ListDescriptor {
        override val uniqueIdentifier = ListDescriptorUniqueIdentifier(100)
        override val typeIdentifier = ListDescriptorTypeIdentifier(200)
        override val config = ListConfig.default
    }

    @Before
    fun setUp() {
        dao = dbRule.db.listDao()
    }

    // region getList
    @Test
    fun `given no list exists, when get list, then returns null`() = runTest {
        val result = dao.getList(testDescriptor)

        assertThat(result).isNull()
    }

    @Test
    fun `given list inserted, when get list, then returns list`() = runTest {
        dao.insertOrUpdateList(testDescriptor, ListState.CAN_LOAD_MORE, LAST_MODIFIED)

        val result = dao.getList(testDescriptor)

        assertThat(result).isNotNull()
        assertThat(result!!.descriptorUniqueIdentifierDbValue).isEqualTo(testDescriptor.uniqueIdentifier.value)
        assertThat(result.descriptorTypeIdentifierDbValue).isEqualTo(testDescriptor.typeIdentifier.value)
        assertThat(result.lastModified).isEqualTo(LAST_MODIFIED)
        assertThat(result.stateDbValue).isEqualTo(ListState.CAN_LOAD_MORE.value)
    }

    // endregion

    // region insertOrUpdateList
    @Test
    fun `given existing list, when update list state, then state is updated`() = runTest {
        dao.insertOrUpdateList(testDescriptor, ListState.CAN_LOAD_MORE, LAST_MODIFIED)

        val updateState = ListState.FETCHED
        val updatedModified = "2025-02-01T00:00:00Z"
        dao.insertOrUpdateList(testDescriptor, updateState, updatedModified)

        val result = dao.getList(testDescriptor)
        assertThat(result).isNotNull()
        assertThat(result!!.lastModified).isEqualTo(updatedModified)
        assertThat(result.stateDbValue).isEqualTo(updateState.value)
    }

    @Test
    fun `given existing list, when update list state, then id is preserved`() = runTest {
        val original = dao.insertOrUpdateList(testDescriptor, ListState.CAN_LOAD_MORE, LAST_MODIFIED)

        val updated = dao.insertOrUpdateList(testDescriptor, ListState.FETCHED, "2025-02-01T00:00:00Z")

        assertThat(updated.id).isEqualTo(original.id)
    }

    // endregion

    // region insertItems / getListItems
    @Test
    fun `given items inserted, when get items, then returns items in insertion order`() = runTest {
        val list = dao.insertOrUpdateList(testDescriptor, ListState.CAN_LOAD_MORE, LAST_MODIFIED)
        val items = listOf(
            ListItemModel(list.id, 30L),
            ListItemModel(list.id, 10L),
            ListItemModel(list.id, 20L)
        )
        dao.insertItems(items)

        val result = dao.getListItems(list.id)

        assertThat(result).hasSize(3)
        assertThat(result.map { it.remoteItemId }).containsExactly(30L, 10L, 20L)
    }

    @Test
    fun `given duplicate items, when insert items, then duplicates are ignored`() = runTest {
        val list = dao.insertOrUpdateList(testDescriptor, ListState.CAN_LOAD_MORE, LAST_MODIFIED)
        dao.insertItems(listOf(
            ListItemModel(list.id, 1L)
        ))

        dao.insertItems(listOf(
            ListItemModel(list.id, 1L),
            ListItemModel(list.id, 2L)
        ))

        val result = dao.getListItems(list.id)
        assertThat(result).hasSize(2)
        assertThat(result.map { it.remoteItemId }).containsExactly(1L, 2L)
    }

    // endregion

    // region getListItemsByDescriptor
    @Test
    fun `given items exist, when get list items, then returns items`() = runTest {
        val list = dao.insertOrUpdateList(testDescriptor, ListState.CAN_LOAD_MORE, lastModified = LAST_MODIFIED)
        dao.insertItems(listOf(
            ListItemModel(listId = list.id, remoteItemId = 10L),
            ListItemModel(listId = list.id, remoteItemId = 20L)
        ))

        val result = dao.getListItems(testDescriptor)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.remoteItemId }).containsExactly(10L, 20L)
    }

    @Test
    fun `given no list exists, when get list items, then returns empty`() = runTest {
        val result = dao.getListItems(testDescriptor)

        assertThat(result).isEmpty()
    }
    // endregion

    // region getListItemsCount
    @Test
    fun `given items exist, when get items count, then returns correct count`() = runTest {
        val list = dao.insertOrUpdateList(testDescriptor, ListState.CAN_LOAD_MORE, LAST_MODIFIED)
        dao.insertItems(listOf(
            ListItemModel(list.id, 1L),
            ListItemModel(list.id, 2L),
            ListItemModel(list.id, 3L)
        ))

        val count = dao.getListItemsCount(list.id)

        assertThat(count).isEqualTo(3L)
    }

    @Test
    fun `given no items exist, when get items count, then returns zero`() = runTest {
        val list = dao.insertOrUpdateList(testDescriptor, ListState.CAN_LOAD_MORE, LAST_MODIFIED)

        val count = dao.getListItemsCount(list.id)

        assertThat(count).isEqualTo(0L)
    }
    // endregion

    // region deleteAndInsertItems
    @Test
    fun `given items exist, when delete items, then items are removed`() = runTest {
        val list = dao.insertOrUpdateList(testDescriptor, ListState.CAN_LOAD_MORE, LAST_MODIFIED)
        dao.insertItems(listOf(
            ListItemModel(list.id, 1L),
            ListItemModel(list.id, 2L)
        ))

        dao.deleteAndInsertItems(list.id, true, emptyList())

        val result = dao.getListItems(list.id)
        assertThat(result).isEmpty()
    }

    @Test
    fun `given items exist, when delete and insert items, then old items replaced`() = runTest {
        val list = dao.insertOrUpdateList(testDescriptor, ListState.CAN_LOAD_MORE, LAST_MODIFIED)
        dao.insertItems(listOf(
            ListItemModel(list.id, 1L),
            ListItemModel(list.id, 2L)
        ))
        val newItems = listOf(
            ListItemModel(list.id, 10L),
            ListItemModel(list.id, 20L),
            ListItemModel(list.id, 30L)
        )

        dao.deleteAndInsertItems(list.id, true, newItems)

        val result = dao.getListItems(list.id)
        assertThat(result).hasSize(3)
        assertThat(result.map { it.remoteItemId }).containsExactly(10L, 20L, 30L)
    }

    @Test
    fun `given items exist, when delete and insert without delete, then items are appended`() = runTest {
        val list = dao.insertOrUpdateList(testDescriptor, ListState.CAN_LOAD_MORE, LAST_MODIFIED)
        dao.insertItems(listOf(
            ListItemModel(list.id, 1L),
            ListItemModel(list.id, 2L)
        ))
        val newItems = listOf(
            ListItemModel(list.id, 3L),
            ListItemModel(list.id, 4L)
        )

        dao.deleteAndInsertItems(list.id, false, newItems)

        val result = dao.getListItems(list.id)
        assertThat(result).hasSize(4)
        assertThat(result.map { it.remoteItemId }).containsExactly(1L, 2L, 3L, 4L)
    }

    // endregion

    private companion object {
        const val LAST_MODIFIED = "2025-01-01T00:00:00Z"
    }
}
