package org.wordpress.android.fluxc.model.list

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val LIST_STATE_TIMEOUT = 60 * 1000 // 1 minute

@Entity(
    tableName = "ListEntity",
    indices = [Index(
        value = ["descriptorUniqueIdentifierDbValue", "descriptorTypeIdentifierDbValue"],
        unique = true
    )]
)
data class ListModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lastModified: String?,
    val descriptorUniqueIdentifierDbValue: Int,
    val descriptorTypeIdentifierDbValue: Int,
    val stateDbValue: Int
)
