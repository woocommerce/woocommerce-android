package org.wordpress.android.fluxc.model.list

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "ListItemEntity",
    primaryKeys = ["listId", "remoteItemId"],
    foreignKeys = [ForeignKey(
        entity = ListModel::class,
        parentColumns = ["id"],
        childColumns = ["listId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ListItemModel(
    val listId: Int,
    val remoteItemId: Long
)
