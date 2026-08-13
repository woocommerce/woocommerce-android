package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

/**
 * A single persisted filter selection ("filter history" entry) for the order or product list.
 *
 * [payload] is a canonical, normalized serialization of the selected filters and doubles as the
 * dedup key: the unique index on ([localSiteId], [filterType], [payload]) combined with
 * `OnConflictStrategy.REPLACE` means re-saving an identical selection replaces the existing row and
 * bumps [dateModified], moving it to the top of the newest-first ordering.
 */
@Entity(
    tableName = "FilterHistory",
    indices = [Index(value = ["localSiteId", "filterType", "payload"], unique = true)]
)
data class FilterHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localSiteId: LocalId,
    val filterType: String,
    val payload: String,
    val readableString: String,
    val dateModified: Long
)
