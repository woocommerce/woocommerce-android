package org.wordpress.android.fluxc.persistence.mappers

import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.CustomText.Restrictions
import org.wordpress.android.fluxc.domain.Addon.TitleFormat
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalDisplay.Images
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalDisplay.RadioButton
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalDisplay.Select
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalPriceType.FlatFee
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalPriceType.PercentageBased
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalPriceType.QuantityBased
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalRestrictions.AnyText
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalRestrictions.Email
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalRestrictions.OnlyLetters
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalRestrictions.OnlyLettersNumbers
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalRestrictions.OnlyNumbers
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalTitleFormat.Hide
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalTitleFormat.Label
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.Checkbox
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.CustomPrice
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.CustomText
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.CustomTextArea
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.FileUpload
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.Heading
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.InputMultiplier
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.LocalType.MultipleChoice
import org.wordpress.android.fluxc.persistence.entity.AddonWithOptions

object FromDatabaseAddonsMapper {
    fun toDomainModel(entity: AddonWithOptions): Addon {
        return when (entity.addon.type) {
            MultipleChoice -> multipleChoice(entity)
            Checkbox -> checkbox(entity)
            CustomText -> customText(entity)
            CustomTextArea -> customTextArea(entity)
            FileUpload -> fileUpload(entity)
            CustomPrice -> customPrice(entity)
            InputMultiplier -> inputMultiplier(entity)
            Heading -> heading(entity)
        }
    }

    private fun multipleChoice(entity: AddonWithOptions) = Addon.MultipleChoice(
        name = entity.addon.name,
        titleFormat = entity.addon.titleFormat.toDomainModel(),
        description = entity.addon.description,
        required = entity.addon.required,
        position = entity.addon.position,
        options = entity.mapOptions(),
        display = entity.addon.display?.toDomainModel() ?: throw MappingDatabaseException(
            "Can't map ${entity.addon.name}. MultipleChoice add-on type has to have `display` defined."
        )
    )

    private fun checkbox(entity: AddonWithOptions) = Addon.Checkbox(
        name = entity.addon.name,
        titleFormat = entity.addon.titleFormat.toDomainModel(),
        description = entity.addon.description,
        required = entity.addon.required,
        position = entity.addon.position,
        options = entity.mapOptions()
    )

    private fun customText(entity: AddonWithOptions) = Addon.CustomText(
        name = entity.addon.name,
        titleFormat = entity.addon.titleFormat.toDomainModel(),
        description = entity.addon.description,
        required = entity.addon.required,
        position = entity.addon.position,
        restrictions = entity.addon.mapRestrictions(),
        price = entity.addon.mapPrice(),
        characterLength = prepareRange(entity.addon.min, entity.addon.max)
    )

    private fun customTextArea(entity: AddonWithOptions) = Addon.CustomTextArea(
        name = entity.addon.name,
        titleFormat = entity.addon.titleFormat.toDomainModel(),
        description = entity.addon.description,
        required = entity.addon.required,
        position = entity.addon.position,
        price = entity.addon.mapPrice(),
        characterLength = prepareRange(entity.addon.min, entity.addon.max)
    )

    private fun fileUpload(entity: AddonWithOptions) = Addon.FileUpload(
        name = entity.addon.name,
        titleFormat = entity.addon.titleFormat.toDomainModel(),
        description = entity.addon.description,
        required = entity.addon.required,
        position = entity.addon.position,
        price = entity.addon.mapPrice()
    )

    private fun customPrice(entity: AddonWithOptions) = Addon.CustomPrice(
        name = entity.addon.name,
        titleFormat = entity.addon.titleFormat.toDomainModel(),
        description = entity.addon.description,
        required = entity.addon.required,
        position = entity.addon.position,
        priceRange = prepareRange(entity.addon.min, entity.addon.max)
    )

    private fun inputMultiplier(entity: AddonWithOptions) = Addon.InputMultiplier(
        name = entity.addon.name,
        titleFormat = entity.addon.titleFormat.toDomainModel(),
        description = entity.addon.description,
        required = entity.addon.required,
        position = entity.addon.position,
        price = entity.addon.mapPrice(),
        quantityRange = prepareRange(entity.addon.min, entity.addon.max)
    )

    private fun heading(entity: AddonWithOptions) = Addon.Heading(
        name = entity.addon.name,
        titleFormat = entity.addon.titleFormat.toDomainModel(),
        description = entity.addon.description,
        required = entity.addon.required,
        position = entity.addon.position
    )

    private fun prepareRange(min: Long?, max: Long?) = if (max == null || max == 0L) {
        null
    } else {
        (min ?: 0)..max
    }

    private fun AddonWithOptions.mapOptions(): List<Addon.HasOptions.Option> {
        return this.options.map { optionEntity ->
            DatabaseAddonOptionMapper.toDomain(optionEntity)
        }
    }

    private fun AddonEntity.mapRestrictions(): Restrictions {
        return if (restrictions != null) {
            when (this.restrictions) {
                AnyText -> Restrictions.AnyText
                OnlyLetters -> Restrictions.OnlyLetters
                OnlyNumbers -> Restrictions.OnlyNumbers
                OnlyLettersNumbers -> Restrictions.OnlyLettersNumbers
                Email -> Restrictions.Email
            }
        } else {
            throw MappingDatabaseException("Can't map $name. CustomText Add-on has to have restrictions defined.")
        }
    }

    private fun AddonEntity.mapPrice(): Addon.HasAdjustablePrice.Price {
        return if (this.priceType != null) {
            Addon.HasAdjustablePrice.Price.Adjusted(
                priceType = this.priceType.mapToDomain(),
                value = this.price.orEmpty()
            )
        } else {
            Addon.HasAdjustablePrice.Price.NotAdjusted
        }
    }

    private fun AddonEntity.LocalTitleFormat.toDomainModel(): TitleFormat {
        return when (this) {
            Label -> TitleFormat.Label
            AddonEntity.LocalTitleFormat.Heading -> TitleFormat.Heading
            Hide -> TitleFormat.Hide
        }
    }

    private fun AddonEntity.LocalDisplay.toDomainModel(): Addon.MultipleChoice.Display {
        return when (this) {
            Select -> Addon.MultipleChoice.Display.Select
            RadioButton -> Addon.MultipleChoice.Display.RadioButton
            Images -> Addon.MultipleChoice.Display.Images
        }
    }

    fun AddonEntity.LocalPriceType.mapToDomain(): Addon.HasAdjustablePrice.Price.Adjusted.PriceType {
        return when (this) {
            FlatFee -> Addon.HasAdjustablePrice.Price.Adjusted.PriceType.FlatFee
            QuantityBased -> Addon.HasAdjustablePrice.Price.Adjusted.PriceType.QuantityBased
            PercentageBased -> Addon.HasAdjustablePrice.Price.Adjusted.PriceType.PercentageBased
        }
    }
}
