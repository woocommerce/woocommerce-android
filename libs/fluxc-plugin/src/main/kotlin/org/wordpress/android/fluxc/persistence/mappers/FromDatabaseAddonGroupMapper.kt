package org.wordpress.android.fluxc.persistence.mappers

import org.wordpress.android.fluxc.domain.GlobalAddonGroup
import org.wordpress.android.fluxc.domain.GlobalAddonGroup.CategoriesRestriction.AllProductsCategories
import org.wordpress.android.fluxc.domain.GlobalAddonGroup.CategoriesRestriction.SpecifiedProductCategories
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupWithAddons
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject

class FromDatabaseAddonGroupMapper @Inject constructor(private val logger: AppLogWrapper) {
    fun toDomainModel(entity: GlobalAddonGroupWithAddons): GlobalAddonGroup {
        return GlobalAddonGroup(
                name = entity.group.name,
                restrictedCategoriesIds = if (entity.group.restrictedCategoriesIds.isEmpty()) {
                    AllProductsCategories
                } else {
                    SpecifiedProductCategories(entity.group.restrictedCategoriesIds)
                },
                addons = entity.addons.mapNotNull { addonEntity ->
                    try {
                        FromDatabaseAddonsMapper.toDomainModel(addonEntity)
                    } catch (exception: MappingDatabaseException) {
                        logger.e(API, exception.message)
                        null
                    }
                }
        )
    }
}
