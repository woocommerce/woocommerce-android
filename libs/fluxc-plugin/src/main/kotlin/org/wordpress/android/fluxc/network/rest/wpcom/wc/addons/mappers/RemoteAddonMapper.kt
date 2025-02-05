package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.mappers

import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteDisplay
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemotePriceType
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteRestrictionsType
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteTitleFormat
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteTitleFormat.Hide
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteTitleFormat.Label
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.Checkbox
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.CustomPrice
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.CustomText
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.CustomTextArea
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.FileUpload
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.Heading
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.InputMultiplier
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.MultipleChoice

object RemoteAddonMapper {
    fun toDomain(dto: RemoteAddonDto): Addon {
        return when (dto.type) {
            MultipleChoice -> multipleChoice(dto)
            Checkbox -> checkbox(dto)
            CustomText -> customText(dto)
            CustomTextArea -> customTextArea(dto)
            FileUpload -> fileUpload(dto)
            CustomPrice -> customPrice(dto)
            InputMultiplier -> inputMultiplier(dto)
            Heading -> heading(dto)
            else -> throw MappingRemoteException("Add-on has to have type")
        }
    }

    private fun multipleChoice(dto: RemoteAddonDto) = Addon.MultipleChoice(
        name = dto.name,
        titleFormat = dto.titleFormat.toDomainModel(),
        description = dto.mapDescription(),
        required = dto.required.asBoolean(),
        position = dto.position,
        options = dto.mapOptions(),
        display = dto.display?.toDomainModel() ?: throw MappingRemoteException(
            "MultipleChoice add-on type has to have `display` defined."
        )
    )

    private fun checkbox(dto: RemoteAddonDto) = Addon.Checkbox(
        name = dto.name,
        titleFormat = dto.titleFormat.toDomainModel(),
        description = dto.mapDescription(),
        required = dto.required.asBoolean(),
        position = dto.position,
        options = dto.mapOptions()
    )

    private fun customText(dto: RemoteAddonDto) = Addon.CustomText(
        name = dto.name,
        titleFormat = dto.titleFormat.toDomainModel(),
        description = dto.mapDescription(),
        required = dto.required.asBoolean(),
        position = dto.position,
        restrictions = dto.mapRestrictions(),
        price = dto.mapPrice(),
        characterLength = prepareRange(dto.min, dto.max)
    )

    private fun customTextArea(dto: RemoteAddonDto) = Addon.CustomTextArea(
        name = dto.name,
        titleFormat = dto.titleFormat.toDomainModel(),
        description = dto.mapDescription(),
        required = dto.required.asBoolean(),
        position = dto.position,
        price = dto.mapPrice(),
        characterLength = prepareRange(dto.min, dto.max)
    )

    private fun fileUpload(dto: RemoteAddonDto) = Addon.FileUpload(
        name = dto.name,
        titleFormat = dto.titleFormat.toDomainModel(),
        description = dto.mapDescription(),
        required = dto.required.asBoolean(),
        position = dto.position,
        price = dto.mapPrice()
    )

    private fun customPrice(dto: RemoteAddonDto) = Addon.CustomPrice(
        name = dto.name,
        titleFormat = dto.titleFormat.toDomainModel(),
        description = dto.mapDescription(),
        required = dto.required.asBoolean(),
        position = dto.position,
        priceRange = prepareRange(dto.min, dto.max)
    )

    private fun inputMultiplier(dto: RemoteAddonDto) = Addon.InputMultiplier(
        name = dto.name,
        titleFormat = dto.titleFormat.toDomainModel(),
        description = dto.mapDescription(),
        required = dto.required.asBoolean(),
        position = dto.position,
        price = dto.mapPrice(),
        quantityRange = prepareRange(dto.min, dto.max)
    )

    private fun heading(dto: RemoteAddonDto) = Addon.Heading(
        name = dto.name,
        titleFormat = dto.titleFormat.toDomainModel(),
        description = dto.mapDescription(),
        required = dto.required.asBoolean(),
        position = dto.position
    )

    private fun RemoteAddonDto.mapDescription(): String? {
        return if (descriptionEnabled.asBoolean()) {
            this.description
        } else {
            null
        }
    }

    private fun RemoteAddonDto.mapOptions(): List<Addon.HasOptions.Option> {
        return this.options?.map { optionDto ->
            RemoteAddonOptionDtoMapper.toDomainModel(optionDto)
        }.orEmpty()
    }

    private fun RemoteTitleFormat?.toDomainModel(): Addon.TitleFormat {
        return when (this) {
            Label -> Addon.TitleFormat.Label
            RemoteTitleFormat.Heading -> Addon.TitleFormat.Heading
            Hide -> Addon.TitleFormat.Hide
            null -> throw MappingRemoteException("Add-on has to have title")
        }
    }

    private fun RemoteDisplay.toDomainModel(): Addon.MultipleChoice.Display {
        return when (this) {
            RemoteDisplay.Select -> Addon.MultipleChoice.Display.Select
            RemoteDisplay.RadioButton -> Addon.MultipleChoice.Display.RadioButton
            RemoteDisplay.Images -> Addon.MultipleChoice.Display.Images
        }
    }

    private fun RemoteAddonDto.mapRestrictions(): Addon.CustomText.Restrictions {
        return if (restrictionsType != null) {
            when (this.restrictionsType) {
                RemoteRestrictionsType.AnyText -> Addon.CustomText.Restrictions.AnyText
                RemoteRestrictionsType.OnlyLetters -> Addon.CustomText.Restrictions.OnlyLetters
                RemoteRestrictionsType.OnlyNumbers -> Addon.CustomText.Restrictions.OnlyNumbers
                RemoteRestrictionsType.OnlyLettersNumbers -> Addon.CustomText.Restrictions.OnlyLettersNumbers
                RemoteRestrictionsType.Email -> Addon.CustomText.Restrictions.Email
            }
        } else {
            throw MappingRemoteException("CustomText Add-on has to have restrictions defined.")
        }
    }

    private fun RemoteAddonDto.mapPrice(): Addon.HasAdjustablePrice.Price {
        return if (this.priceType != null) {
            Addon.HasAdjustablePrice.Price.Adjusted(
                priceType = this.priceType.mapToDomain(),
                value = this.price.orEmpty()
            )
        } else {
            Addon.HasAdjustablePrice.Price.NotAdjusted
        }
    }

    private fun RemotePriceType.mapToDomain(): PriceType {
        return when (this) {
            RemotePriceType.FlatFee -> PriceType.FlatFee
            RemotePriceType.QuantityBased -> PriceType.QuantityBased
            RemotePriceType.PercentageBased -> PriceType.PercentageBased
        }
    }

    private fun Int?.asBoolean(): Boolean {
        return this == 1
    }

    private fun prepareRange(min: Long, max: Long): LongRange? {
        return if (max == 0L) null else (min..max)
    }
}
