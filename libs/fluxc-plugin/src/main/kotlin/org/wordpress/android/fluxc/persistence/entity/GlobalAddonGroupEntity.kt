package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

data class GlobalAddonGroupWithAddons(
    @Embedded val group: GlobalAddonGroupEntity,
    @Relation(
            parentColumn = "globalGroupLocalId",
            entityColumn = "globalGroupLocalId",
            entity = AddonEntity::class
    )
    val addons: List<AddonWithOptions>
)

@Entity
data class GlobalAddonGroupEntity(
    @PrimaryKey(autoGenerate = true) val globalGroupLocalId: Long = 0,
    val name: String,
    val restrictedCategoriesIds: List<Long>,
    val localSiteId: LocalId
)
