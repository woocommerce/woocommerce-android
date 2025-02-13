package org.wordpress.android.fluxc.persistence.mappers

import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType.FlatFee
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType.PercentageBased
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType.QuantityBased
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity
import org.wordpress.android.fluxc.persistence.mappers.FromDatabaseAddonsMapper.mapToDomain

object DatabaseAddonOptionMapper {
    fun toLocalEntity(domain: Addon.HasOptions.Option, addonLocalId: Long): AddonOptionEntity {
        return AddonOptionEntity(
                addonLocalId = addonLocalId,
                label = domain.label,
                image = domain.image,
                priceType = domain.price.type(),
                price = domain.price.value
        )
    }

    fun toDomain(entity: AddonOptionEntity): Addon.HasOptions.Option {
        return Addon.HasOptions.Option(
                price = Addon.HasAdjustablePrice.Price.Adjusted(
                        priceType = entity.priceType.mapToDomain(),
                        value = entity.price.orEmpty()
                ),
                label = entity.label,
                image = entity.image
        )
    }

    private fun Addon.HasAdjustablePrice.Price.Adjusted.type(): AddonEntity.LocalPriceType {
        return when (this.priceType) {
            FlatFee -> AddonEntity.LocalPriceType.FlatFee
            QuantityBased -> AddonEntity.LocalPriceType.QuantityBased
            PercentageBased -> AddonEntity.LocalPriceType.PercentageBased
        }
    }
}
