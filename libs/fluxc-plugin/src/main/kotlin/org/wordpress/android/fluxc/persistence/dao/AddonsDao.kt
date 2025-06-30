package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.HasOptions
import org.wordpress.android.fluxc.domain.GlobalAddonGroup
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity
import org.wordpress.android.fluxc.persistence.entity.AddonWithOptions
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupWithAddons
import org.wordpress.android.fluxc.persistence.entity.ProductBasedIdentification
import org.wordpress.android.fluxc.persistence.mappers.DatabaseAddonOptionMapper
import org.wordpress.android.fluxc.persistence.mappers.ToDatabaseAddonGroupMapper
import org.wordpress.android.fluxc.persistence.mappers.ToDatabaseAddonsMapper

@Dao
abstract class AddonsDao {
    @Transaction
    @Query("SELECT * FROM GlobalAddonGroupEntity WHERE localSiteId = :localSiteId")
    abstract fun observeGlobalAddonsForSite(localSiteId: LocalId): Flow<List<GlobalAddonGroupWithAddons>>

    @Transaction
    @Query("SELECT * FROM AddonEntity WHERE localSiteId = :localSiteId AND productRemoteId = :productRemoteId")
    abstract fun observeSingleProductAddons(
        localSiteId: LocalId,
        productRemoteId: RemoteId
    ): Flow<List<AddonWithOptions>>

    @Insert
    abstract suspend fun insertGroup(globalAddonGroupEntity: GlobalAddonGroupEntity): Long

    @Insert
    abstract suspend fun insertAddons(addonEntities: AddonEntity): Long

    @Insert
    abstract suspend fun insertAddonOptions(vararg addonOptions: AddonOptionEntity)

    @Query("DELETE FROM GlobalAddonGroupEntity WHERE localSiteId = :localSiteId")
    abstract suspend fun deleteGlobalAddonsForSite(localSiteId: LocalId)

    @Query("DELETE FROM AddonEntity WHERE productRemoteId = :productRemoteId AND localSiteId = :localSiteId")
    abstract suspend fun deleteAddonsForSpecifiedProduct(productRemoteId: RemoteId, localSiteId: LocalId)

    @Transaction
    open suspend fun cacheGroups(
        globalAddonGroups: List<GlobalAddonGroup>,
        localSiteId: LocalId
    ) {
        deleteGlobalAddonsForSite(localSiteId)

        globalAddonGroups.forEach { group ->
            val entity = ToDatabaseAddonGroupMapper.toEntityModel(
                    domain = group,
                    localSiteId = localSiteId
            )
            val globalAddonGroupEntityId = insertGroup(entity)
            insertAddonEntity(group.addons, globalAddonGroupEntityId)
        }
    }

    @Transaction
    open suspend fun cacheProductAddons(
        productRemoteId: RemoteId,
        localSiteId: LocalId,
        addons: List<Addon>
    ) {
        deleteAddonsForSpecifiedProduct(productRemoteId = productRemoteId, localSiteId = localSiteId)

        addons.forEach { addon ->

            val addonEntity = ToDatabaseAddonsMapper.toEntityModel(
                    domain = addon,
                    productBasedIdentification = ProductBasedIdentification(
                            localSiteId = localSiteId,
                            productRemoteId = productRemoteId
                    )
            )
            val addonEntityId = insertAddons(addonEntity)

            if (addon is HasOptions) {
                insertAddonOptionEntity(addon.options, addonEntityId)
            }
        }
    }

    private suspend fun insertAddonEntity(
        addons: List<Addon>,
        groupId: Long
    ) {
        addons.forEach { addon ->
            val addonEntity = ToDatabaseAddonsMapper.toEntityModel(
                    domain = addon,
                    globalGroupLocalId = groupId
            )
            val addonEntityId = insertAddons(addonEntity)

            if (addon is HasOptions) {
                insertAddonOptionEntity(addon.options, addonEntityId)
            }
        }
    }

    private suspend fun insertAddonOptionEntity(
        options: List<HasOptions.Option>?,
        addonEntityId: Long
    ) {
        options?.forEach { addonOption ->
            DatabaseAddonOptionMapper.toLocalEntity(addonOption, addonEntityId)
            val addonOptionEntity = DatabaseAddonOptionMapper.toLocalEntity(
                    addonLocalId = addonEntityId,
                    domain = addonOption
            )
            insertAddonOptions(addonOptionEntity)
        }
    }
}
