package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.mappers

import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemotePriceType.FlatFee
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemotePriceType.PercentageBased
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemotePriceType.QuantityBased
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteOption

object RemoteAddonOptionDtoMapper {
    fun toDomainModel(dto: RemoteOption): Addon.HasOptions.Option {
        return Addon.HasOptions.Option(
                label = dto.label,
                image = dto.image,
                price = dto.mapPrice()
        )
    }

    private fun RemoteOption.mapPrice(): Addon.HasAdjustablePrice.Price.Adjusted {
        return Addon.HasAdjustablePrice.Price.Adjusted(
                priceType = when (this.priceType) {
                    FlatFee -> Addon.HasAdjustablePrice.Price.Adjusted.PriceType.FlatFee
                    QuantityBased -> Addon.HasAdjustablePrice.Price.Adjusted.PriceType.QuantityBased
                    PercentageBased -> Addon.HasAdjustablePrice.Price.Adjusted.PriceType.PercentageBased
                },
                value = this.price.orEmpty()
        )
    }
}
