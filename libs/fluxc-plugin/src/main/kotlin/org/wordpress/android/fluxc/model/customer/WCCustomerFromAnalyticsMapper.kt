package org.wordpress.android.fluxc.model.customer

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerFromAnalyticsDTO
import org.wordpress.android.fluxc.persistence.entity.CustomerFromAnalyticsEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCCustomerFromAnalyticsMapper @Inject constructor() {
    fun mapDTOToEntity(site: SiteModel, dto: CustomerFromAnalyticsDTO): CustomerFromAnalyticsEntity {
        return CustomerFromAnalyticsEntity(
            localSiteId = site.localId(),
            id = dto.id ?: 0L,
            userId = dto.userId ?: 0L,
            avgOrderValue = dto.avgOrderValue ?: 0.0,
            city = dto.city.orEmpty(),
            country = dto.country.orEmpty(),
            dateLastActive = dto.dateLastActive.orEmpty(),
            dateLastActiveGmt = dto.dateLastActiveGmt.orEmpty(),
            dateLastOrder = dto.dateLastOrder.orEmpty(),
            dateRegistered = dto.dateRegistered.orEmpty(),
            dateRegisteredGmt = dto.dateRegisteredGmt.orEmpty(),
            email = dto.email.orEmpty(),
            name = dto.name.orEmpty(),
            ordersCount = dto.ordersCount ?: 0,
            postcode = dto.postcode.orEmpty(),
            state = dto.state.orEmpty(),
            totalSpend = dto.totalSpend ?: 0.0,
            username = dto.username.orEmpty()
        )
    }

    fun mapEntityToModel(entity: CustomerFromAnalyticsEntity): WCCustomerFromAnalytics {
        return WCCustomerFromAnalytics(
            avgOrderValue = entity.avgOrderValue,
            city = entity.city,
            country = entity.country,
            dateLastActive = entity.dateLastActive,
            dateLastActiveGmt = entity.dateLastActiveGmt,
            dateLastOrder = entity.dateLastOrder,
            dateRegistered = entity.dateRegistered,
            dateRegisteredGmt = entity.dateRegisteredGmt,
            email = entity.email,
            id = entity.id,
            name = entity.name,
            ordersCount = entity.ordersCount,
            postcode = entity.postcode,
            state = entity.state,
            totalSpend = entity.totalSpend,
            userId = entity.userId,
            username = entity.username
        )
    }
}
