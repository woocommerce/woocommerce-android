package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.model.metadata.WCMetaData

/**
 * The MetaDataEntity table is used to store viewable wordpress metadata. WordPress metadata
 * can potentially be quite large, so we keep it separate from the order.
 *
 * For now, this stores metadata for orders and products.
 */
@Entity(
    tableName = "MetaData",
    primaryKeys = ["localSiteId", "parentItemId", "id"]
)
data class MetaDataEntity(
    @ColumnInfo(name = "localSiteId")
    val localSiteId: LocalId,
    val id: Long,
    val parentItemId: Long,
    val key: String,
    val value: String,
    @ColumnInfo(defaultValue = "ORDER") // We default to ORDER for backwards compatibility
    val type: MetaDataParentItemType
) {
    fun toDomainModel() = WCMetaData(
        id = id,
        key = key,
        value = value
    )

    companion object {
        fun fromDomainModel(
            metaData: WCMetaData,
            localSiteId: LocalId,
            parentItemId: Long,
            parentItemType: MetaDataParentItemType
        ) = MetaDataEntity(
            localSiteId = localSiteId,
            id = metaData.id,
            parentItemId = parentItemId,
            key = metaData.key,
            value = metaData.value.jsonValue.toString(),
            type = parentItemType
        )
    }
}
