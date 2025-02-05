package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.mappers

import org.wordpress.android.fluxc.domain.GlobalAddonGroup
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.dto.AddOnGroupDto
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject

internal class RemoteGlobalAddonGroupMapper @Inject constructor(private val appLogWrapper: AppLogWrapper) {
    fun toDomain(dto: AddOnGroupDto): GlobalAddonGroup {
        return GlobalAddonGroup(
                name = dto.name,
                restrictedCategoriesIds = if (dto.categoryIds.isNullOrEmpty()) {
                    GlobalAddonGroup.CategoriesRestriction.AllProductsCategories
                } else {
                    GlobalAddonGroup.CategoriesRestriction.SpecifiedProductCategories(dto.categoryIds)
                },
                addons = dto.addons.mapNotNull { dtoAddon ->
                    try {
                        RemoteAddonMapper.toDomain(dtoAddon)
                    } catch (exception: MappingRemoteException) {
                        appLogWrapper.e(API, "Exception while parsing $dtoAddon: ${exception.message}")
                        null
                    }
                }
        )
    }
}
