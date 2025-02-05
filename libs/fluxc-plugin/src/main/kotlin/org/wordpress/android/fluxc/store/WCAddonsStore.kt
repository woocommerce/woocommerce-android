package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.GlobalAddonGroup
import org.wordpress.android.fluxc.domain.GlobalAddonGroup.CategoriesRestriction.AllProductsCategories
import org.wordpress.android.fluxc.domain.GlobalAddonGroup.CategoriesRestriction.SpecifiedProductCategories
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.AddOnsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.mappers.RemoteGlobalAddonGroupMapper
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.entity.AddonWithOptions
import org.wordpress.android.fluxc.persistence.mappers.FromDatabaseAddonGroupMapper
import org.wordpress.android.fluxc.persistence.mappers.FromDatabaseAddonsMapper
import org.wordpress.android.fluxc.persistence.mappers.MappingDatabaseException
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCAddonsStore @Inject internal constructor(
    private val restClient: AddOnsRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val dao: AddonsDao,
    private val remoteGlobalAddonGroupMapper: RemoteGlobalAddonGroupMapper,
    private val fromDatabaseAddonGroupMapper: FromDatabaseAddonGroupMapper,
    private val logger: AppLogWrapper
) {
    suspend fun fetchAllGlobalAddonsGroups(site: SiteModel): WooResult<Unit> {
        return coroutineEngine.withDefaultContext(API, this, "fetchGlobalAddonsGroups") {
            val response = restClient.fetchGlobalAddOnGroups(site)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val globalAddonGroupsDto = response.result

                    val domain = globalAddonGroupsDto.map { dtoGroup ->
                        remoteGlobalAddonGroupMapper.toDomain(dtoGroup)
                    }

                    dao.cacheGroups(domain, site.localId())
                    WooResult(Unit)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    fun observeProductSpecificAddons(site: SiteModel, productRemoteId: Long): Flow<List<Addon>> {
        return dao.observeSingleProductAddons(site.localId(), RemoteId(productRemoteId))
                .map {
                    it.map { entityAddon ->
                        FromDatabaseAddonsMapper.toDomainModel(entityAddon)
                    }
                }
    }

    fun observeAllAddonsForProduct(site: SiteModel, product: WCProductModel): Flow<List<Addon>> {
        return dao.observeGlobalAddonsForSite(localSiteId = site.localId())
                .map { globalGroupsEntities ->
                    val domainGroup = globalGroupsEntities.map { globalGroupEntity ->
                        fromDatabaseAddonGroupMapper.toDomainModel(
                                globalGroupEntity
                        )
                    }
                    getAddonsFromGlobalGroups(domainGroup, product)
                }
                .combine(
                        dao.observeSingleProductAddons(
                                localSiteId = site.localId(),
                                productRemoteId = product.remoteId
                        ).map { addonEntities ->
                            addonEntities.mapNotNull { addonEntity ->
                                mapEntityToAddonSafely(addonEntity)
                            }
                        }
                ) { fromGlobalAddons, fromSingleProductAddons ->
                    fromGlobalAddons + fromSingleProductAddons
                }
    }

    private fun mapEntityToAddonSafely(addonEntity: AddonWithOptions): Addon? {
        return try {
            FromDatabaseAddonsMapper.toDomainModel(addonEntity)
        } catch (exception: MappingDatabaseException) {
            logger.e(API, exception.message)
            null
        }
    }

    private fun getAddonsFromGlobalGroups(
        globalGroups: List<GlobalAddonGroup>,
        product: WCProductModel
    ): List<Addon> {
        return globalGroups.filter { globalGroup ->
            when (globalGroup.restrictedCategoriesIds) {
                AllProductsCategories -> true
                is SpecifiedProductCategories -> globalGroup.restrictedCategoriesIds.appliesToProduct(product)
            }
        }.flatMap { it.addons }
    }

    private fun SpecifiedProductCategories.appliesToProduct(product: WCProductModel): Boolean {
        val productCategoriesIds = product.getCategoryList().map { it.id }

        return this.productCategories.intersect(productCategoriesIds).isNotEmpty()
    }
}
