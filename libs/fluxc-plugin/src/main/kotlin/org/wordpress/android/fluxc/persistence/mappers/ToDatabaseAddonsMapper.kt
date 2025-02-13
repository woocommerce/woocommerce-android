package org.wordpress.android.fluxc.persistence.mappers

import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.CustomText.Restrictions
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.PriceType
import org.wordpress.android.fluxc.domain.Addon.MultipleChoice.Display
import org.wordpress.android.fluxc.domain.Addon.TitleFormat
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.ProductBasedIdentification

object ToDatabaseAddonsMapper {
    fun toEntityModel(
        domain: Addon,
        globalGroupLocalId: Long? = null,
        productBasedIdentification: ProductBasedIdentification? = null
    ): AddonEntity {
        check(globalGroupLocalId != null || productBasedIdentification != null) {
            "Addon has to be identified with a Group or a Product"
        }

        val range = domain.mapRange()
        return AddonEntity(
                globalGroupLocalId = globalGroupLocalId,
                productRemoteId = productBasedIdentification?.productRemoteId,
                localSiteId = productBasedIdentification?.localSiteId,
                titleFormat = domain.titleFormat.toLocalEntity(),
                restrictions = domain.mapRestrictions(),
                priceType = domain.mapPriceType(),
                type = domain.mapType(),
                display = domain.mapDisplay(),
                name = domain.name,
                description = domain.description,
                required = domain.required,
                position = domain.position,
                price = domain.mapPrice(),
                min = range?.first,
                max = range?.second
        )
    }

    private fun TitleFormat.toLocalEntity(): AddonEntity.LocalTitleFormat {
        return when (this) {
            TitleFormat.Label -> AddonEntity.LocalTitleFormat.Label
            TitleFormat.Heading -> AddonEntity.LocalTitleFormat.Heading
            TitleFormat.Hide -> AddonEntity.LocalTitleFormat.Hide
        }
    }

    private fun Addon.mapRestrictions(): AddonEntity.LocalRestrictions? {
        return if (this is Addon.CustomText) {
            when (this.restrictions) {
                Restrictions.AnyText -> AddonEntity.LocalRestrictions.AnyText
                Restrictions.OnlyLetters -> AddonEntity.LocalRestrictions.OnlyLetters
                Restrictions.OnlyNumbers -> AddonEntity.LocalRestrictions.OnlyNumbers
                Restrictions.OnlyLettersNumbers -> AddonEntity.LocalRestrictions.OnlyLettersNumbers
                Restrictions.Email -> AddonEntity.LocalRestrictions.Email
            }
        } else {
            null
        }
    }

    private fun Addon.mapPriceType(): AddonEntity.LocalPriceType? {
        return (this as? HasAdjustablePrice).let {
            (it?.price as? Adjusted)?.priceType
        }?.let { priceType ->
            when (priceType) {
                PriceType.FlatFee -> AddonEntity.LocalPriceType.FlatFee
                PriceType.QuantityBased -> AddonEntity.LocalPriceType.QuantityBased
                PriceType.PercentageBased -> AddonEntity.LocalPriceType.PercentageBased
            }
        }
    }

    private fun Addon.mapType(): AddonEntity.LocalType {
        return when (this) {
            is Addon.Checkbox -> AddonEntity.LocalType.Checkbox
            is Addon.CustomPrice -> AddonEntity.LocalType.CustomPrice
            is Addon.CustomText -> AddonEntity.LocalType.CustomText
            is Addon.CustomTextArea -> AddonEntity.LocalType.CustomTextArea
            is Addon.FileUpload -> AddonEntity.LocalType.FileUpload
            is Addon.Heading -> AddonEntity.LocalType.Heading
            is Addon.InputMultiplier -> AddonEntity.LocalType.InputMultiplier
            is Addon.MultipleChoice -> AddonEntity.LocalType.MultipleChoice
        }
    }

    private fun Addon.mapDisplay(): AddonEntity.LocalDisplay? {
        return if (this is Addon.MultipleChoice) {
            when (this.display) {
                Display.Select -> AddonEntity.LocalDisplay.Select
                Display.RadioButton -> AddonEntity.LocalDisplay.RadioButton
                Display.Images -> AddonEntity.LocalDisplay.Images
            }
        } else {
            null
        }
    }

    private fun Addon.mapPrice(): String? =
            (this as? HasAdjustablePrice)
                    ?.let { it.price as? Adjusted }
                    ?.value

    private fun Addon.mapRange(): Pair<Long, Long>? {
        return when (this) {
            is Addon.CustomText -> mapRangeToPair(characterLength)
            is Addon.CustomTextArea -> mapRangeToPair(characterLength)
            is Addon.InputMultiplier -> mapRangeToPair(quantityRange)
            is Addon.CustomPrice -> mapRangeToPair(priceRange)
            else -> null
        }
    }

    private fun mapRangeToPair(characterLength: LongRange?) =
            if (characterLength != null) {
                characterLength.first to characterLength.last
            } else {
                null
            }
}
