package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(
    tableName = "InboxNoteActions",
    foreignKeys = [ForeignKey(
        entity = InboxNoteEntity::class,
        parentColumns = ["localId"],
        childColumns = ["inboxNoteLocalId"],
        onDelete = ForeignKey.CASCADE
    )],
    primaryKeys = ["remoteId", "inboxNoteLocalId"]
)
data class InboxNoteActionEntity(
    val remoteId: Long,
    @ColumnInfo(index = true) val inboxNoteLocalId: Long,
    val localSiteId: LocalId,
    val name: String,
    val label: String,
    val url: String,
    val query: String? = null,
    val status: String? = null,
    val primary: Boolean = false,
    val actionedText: String? = null
)
