package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemModel
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

@Dao
internal abstract class ListDao {
    suspend fun getList(descriptor: ListDescriptor): ListModel? =
        getList(descriptor.uniqueIdentifier.value, descriptor.typeIdentifier.value)

    @Query(
        """
        SELECT * FROM ListEntity
        WHERE descriptorUniqueIdentifierDbValue = :uniqueIdentifier
        AND descriptorTypeIdentifierDbValue = :typeIdentifier
        """
    )
    protected abstract suspend fun getList(uniqueIdentifier: Int, typeIdentifier: Int): ListModel?

    @Transaction
    open suspend fun insertOrUpdateList(
        descriptor: ListDescriptor,
        state: ListState,
        lastModified: String = DateTimeUtils.iso8601FromDate(Date())
    ): ListModel {
        val existing = getList(descriptor)
        return if (existing != null) {
            updateList(existing.id, lastModified, state.value)
            existing.copy(
                lastModified = lastModified,
                stateDbValue = state.value
            )
        } else {
            val new = ListModel(
                lastModified = lastModified,
                descriptorUniqueIdentifierDbValue = descriptor.uniqueIdentifier.value,
                descriptorTypeIdentifierDbValue = descriptor.typeIdentifier.value,
                stateDbValue = state.value
            )
            val rowId = insertList(new)
            new.copy(id = rowId.toInt())
        }
    }

    @Insert
    protected abstract suspend fun insertList(list: ListModel): Long

    /**
     * Marks every cached list sharing [typeIdentifier] as [ListState.NEEDS_REFRESH], except the list
     * with [excludedUniqueIdentifier], so they refetch the next time they are consumed. Each list's
     * [ListModel.lastModified] is left untouched so it keeps reflecting when the list was actually
     * fetched (marking a list stale is not a fetch).
     */
    suspend fun markListsOfTypeNeedRefresh(typeIdentifier: Int, excludedUniqueIdentifier: Int) =
        updateListStatesOfTypeExcept(
            typeIdentifier = typeIdentifier,
            excludedUniqueIdentifier = excludedUniqueIdentifier,
            stateDbValue = ListState.NEEDS_REFRESH.value
        )

    @Query(
        """
        UPDATE ListEntity
        SET stateDbValue = :stateDbValue
        WHERE descriptorTypeIdentifierDbValue = :typeIdentifier
        AND descriptorUniqueIdentifierDbValue != :excludedUniqueIdentifier
        """
    )
    protected abstract suspend fun updateListStatesOfTypeExcept(
        typeIdentifier: Int,
        excludedUniqueIdentifier: Int,
        stateDbValue: Int
    )

    @Query(
        """
        UPDATE ListEntity
        SET lastModified = :lastModified, stateDbValue = :stateDbValue
        WHERE id = :id
        """
    )
    protected abstract suspend fun updateList(id: Int, lastModified: String, stateDbValue: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertItems(items: List<ListItemModel>)

    @Query(
        """
        SELECT * FROM ListItemEntity
        WHERE listId = :listId
        ORDER BY rowid ASC
        """
    )
    abstract suspend fun getListItems(listId: Int): List<ListItemModel>

    suspend fun getListItems(descriptor: ListDescriptor): List<ListItemModel> =
        getListItems(descriptor.uniqueIdentifier.value, descriptor.typeIdentifier.value)

    @Query(
        """
        SELECT i.* FROM ListItemEntity i
        INNER JOIN ListEntity l ON i.listId = l.id
        WHERE l.descriptorUniqueIdentifierDbValue = :uniqueIdentifier
        AND l.descriptorTypeIdentifierDbValue = :typeIdentifier
        ORDER BY i.rowid ASC
        """
    )
    protected abstract suspend fun getListItems(
        uniqueIdentifier: Int,
        typeIdentifier: Int
    ): List<ListItemModel>

    @Query(
        """
        SELECT COUNT(*) FROM ListItemEntity
        WHERE listId = :listId
        """
    )
    abstract suspend fun getListItemsCount(listId: Int): Long

    @Query(
        """
        DELETE FROM ListItemEntity
        WHERE listId = :listId
        """
    )
    protected abstract suspend fun deleteItems(listId: Int)

    @Transaction
    open suspend fun deleteAndInsertItems(
        listId: Int,
        shouldDelete: Boolean,
        items: List<ListItemModel>
    ) {
        if (shouldDelete) deleteItems(listId)
        insertItems(items)
    }
}
