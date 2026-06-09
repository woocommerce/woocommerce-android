package com.woocommerce.android.ui.orders.wooshippinglabels.networking

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.woocommerce.android.extensions.snakeToCamelCase
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AccountSettingsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AddressNormalizationModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodOptions
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PurchasedLabelData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingLabelPaperSize
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRatesDatasourceMapper
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingSelectedRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.DestinationAddressDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.ShippingRateSurchargeDTO
import com.woocommerce.android.util.StringUtils.combineStrings
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.WooShippingLabelEntity
import org.wordpress.android.fluxc.persistence.entity.WooShippingShipmentEntity
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

class WooShippingNetworkingMapper @Inject constructor(
    private val ratesMapper: WooShippingRatesDatasourceMapper
) {
    operator fun invoke(accountSettingsDTO: AccountSettingsDTO): AccountSettingsModel {
        return with(accountSettingsDTO) {
            AccountSettingsModel(
                storeOptions = StoreOptionsModel(
                    currencySymbol = storeOptions.currencySymbol.orEmpty(),
                    dimensionUnit = storeOptions.dimensionUnit.orEmpty(),
                    weightUnit = storeOptions.weightUnit.orEmpty(),
                    originCountry = storeOptions.originCountry.orEmpty()
                ),
                paymentMethodOptions = PaymentMethodOptions(
                    selectedPaymentId = formData.selectedPaymentId,
                    paymentMethods = formMeta.paymentMethods.map { paymentMethod ->
                        PaymentMethodModel(
                            paymentMethodId = paymentMethod.paymentMethodId,
                            name = paymentMethod.name,
                            cardType = paymentMethod.cardType,
                            cardDigits = paymentMethod.cardDigits,
                            expiry = paymentMethod.expiry
                        )
                    },
                    addPaymentMethodUrl = formMeta.addPaymentMethodUrl,
                    emailReceipts = formData.emailReceipts
                ),
                canManagePayments = formMeta.canManagePayments,
                canEditSettings = formMeta.canEditSettings,
                storeOwnerName = formMeta.masterUserName,
                storeOwnerUsername = formMeta.masterUserWpcomLogin,
                paperSize = formData.paperSize ?: WooShippingLabelPaperSize.LABEL,
                lastOrderCompleted = userMeta.lastOrderCompleted
            )
        }
    }

    operator fun invoke(shippingLabelDTO: ShippingLabelDTO): ShippingLabelModel {
        return ShippingLabelModel(
            labelId = shippingLabelDTO.labelId ?: 0,
            tracking = shippingLabelDTO.tracking.orEmpty(),
            refundableAmount = shippingLabelDTO.refundableAmount ?: BigDecimal.ZERO,
            status = shippingLabelDTO.status,
            error = shippingLabelDTO.error,
            created = shippingLabelDTO.created?.let { Date(it) },
            carrierId = shippingLabelDTO.carrierId.orEmpty(),
            serviceName = shippingLabelDTO.serviceName.orEmpty(),
            commercialInvoiceUrl = shippingLabelDTO.commercialInvoiceUrl.orEmpty(),
            isCommercialInvoiceSubmittedElectronically = shippingLabelDTO
                .isCommercialInvoiceSubmittedElectronically == true,
            packageName = shippingLabelDTO.packageName.orEmpty(),
            isLetter = shippingLabelDTO.isLetter == true,
            productNames = shippingLabelDTO.productNames.orEmpty(),
            productIds = shippingLabelDTO.productIds.orEmpty(),
            shipmentId = shippingLabelDTO.shipmentId.orEmpty(),
            receiptItemId = shippingLabelDTO.receiptItemId ?: 0,
            createdDate = shippingLabelDTO.createdDate?.let { Date(it) },
            mainReceiptId = shippingLabelDTO.mainReceiptId ?: 0,
            rate = shippingLabelDTO.rate ?: BigDecimal.ZERO,
            currency = shippingLabelDTO.currency.orEmpty(),
            expiryDate = shippingLabelDTO.expiryDate ?: 0,
            usedDate = shippingLabelDTO.usedDate,
            refund = shippingLabelDTO.refund?.let { refund ->
                ShippingLabelModel.Refund(status = refund.status, requestDate = refund.requestDate?.let { Date(it) })
            }
        )
    }

    operator fun invoke(
        shippingLabelDTO: ShippingLabelDTO,
        site: SiteModel,
        orderId: Long
    ): WooShippingLabelEntity {
        return WooShippingLabelEntity(
            localSiteId = site.localId(),
            orderId = LocalOrRemoteId.RemoteId(orderId),
            labelId = shippingLabelDTO.labelId ?: 0,
            tracking = shippingLabelDTO.tracking.orEmpty(),
            refundableAmount = shippingLabelDTO.refundableAmount ?: BigDecimal.ZERO,
            status = shippingLabelDTO.status.name,
            created = shippingLabelDTO.created?.let { Date(it) },
            carrierId = shippingLabelDTO.carrierId.orEmpty(),
            serviceName = shippingLabelDTO.serviceName.orEmpty(),
            commercialInvoiceUrl = shippingLabelDTO.commercialInvoiceUrl.orEmpty(),
            isCommercialInvoiceSubmittedElectronically = shippingLabelDTO
                .isCommercialInvoiceSubmittedElectronically == true,
            packageName = shippingLabelDTO.packageName.orEmpty(),
            isLetter = shippingLabelDTO.isLetter == true,
            productNames = shippingLabelDTO.productNames.orEmpty(),
            productIds = shippingLabelDTO.productIds.orEmpty(),
            shipmentId = shippingLabelDTO.shipmentId.orEmpty(),
            receiptItemId = shippingLabelDTO.receiptItemId ?: 0,
            createdDate = shippingLabelDTO.createdDate?.let { Date(it) },
            mainReceiptId = shippingLabelDTO.mainReceiptId ?: 0,
            rate = shippingLabelDTO.rate ?: BigDecimal.ZERO,
            currency = shippingLabelDTO.currency.orEmpty(),
            expiryDate = shippingLabelDTO.expiryDate ?: 0,
            usedDate = shippingLabelDTO.usedDate,
            refund = shippingLabelDTO.refund?.let { refund ->
                WooShippingLabelEntity.Refund(
                    status = refund.status,
                    requestDate = refund.requestDate?.let { Date(it) }
                )
            },
            hazmatCategory = null,
            originAddress = null,
            destinationAddress = null,
        )
    }

    operator fun invoke(
        shippingLabelData: ShippingLabelDataDTO,
        site: SiteModel,
        orderId: Long
    ): List<WooShippingLabelEntity> {
        fun key(shipmentId: String) = "shipment_$shipmentId"

        return shippingLabelData.currentOrderLabels?.map { shippingLabelDTO ->
            val shipmentKey = shippingLabelDTO.shipmentId?.let { key(it) }
            val hazmatCategory = shippingLabelData.storedData?.selectedHazmat?.get(shipmentKey)?.category
            val originAddress = shippingLabelData.storedData?.selectedOrigin?.get(shipmentKey)?.let {
                WooShippingLabelEntity.Address(
                    company = it.company,
                    name = it.name,
                    phone = it.phone,
                    countryCode = it.country,
                    state = it.state,
                    address1 = it.address,
                    address2 = it.address2,
                    city = it.city,
                    postcode = it.postcode,
                    email = ""
                )
            }
            val destinationAddress = shippingLabelData.storedData?.selectedDestination?.get(shipmentKey)?.let {
                WooShippingLabelEntity.Address(
                    company = it.company,
                    name = it.name,
                    phone = it.phone,
                    countryCode = it.country,
                    state = it.state,
                    address1 = it.address,
                    address2 = it.address2,
                    city = it.city,
                    postcode = it.postcode,
                    email = "" // We set the email later from the order details
                )
            }

            invoke(shippingLabelDTO, site, orderId).copy(
                hazmatCategory = hazmatCategory,
                originAddress = originAddress,
                destinationAddress = destinationAddress
            )
        }.orEmpty()
    }

    operator fun invoke(
        purchasedShippingLabelResponseDTO: PurchasedShippingLabelResponseDTO,
        site: SiteModel,
        orderId: Long
    ): List<WooShippingLabelEntity> {
        fun key(shipmentId: String) = "shipment_$shipmentId"

        return purchasedShippingLabelResponseDTO.labels.map { shippingLabelDTO ->
            val shipmentKey = shippingLabelDTO.shipmentId?.let { key(it) }
            val hazmatCategory = purchasedShippingLabelResponseDTO.selectedHazmat[shipmentKey]?.category
            val originAddress = purchasedShippingLabelResponseDTO.selectedOrigin[shipmentKey]?.let {
                WooShippingLabelEntity.Address(
                    company = it.company,
                    name = it.name,
                    phone = it.phone,
                    countryCode = it.country,
                    state = it.state,
                    address1 = it.address,
                    address2 = it.address2,
                    city = it.city,
                    postcode = it.postcode,
                    email = ""
                )
            }
            val destinationAddress = purchasedShippingLabelResponseDTO.selectedDestination[shipmentKey]?.let {
                WooShippingLabelEntity.Address(
                    company = it.company,
                    name = it.name,
                    phone = it.phone,
                    countryCode = it.country,
                    state = it.state,
                    address1 = it.address,
                    address2 = it.address2,
                    city = it.city,
                    postcode = it.postcode,
                    email = "" // We set the email later from the order details
                )
            }

            invoke(shippingLabelDTO, site, orderId).copy(
                hazmatCategory = hazmatCategory,
                originAddress = originAddress,
                destinationAddress = destinationAddress
            )
        }
    }

    operator fun invoke(labelEntity: WooShippingLabelEntity): ShippingLabelModel {
        return ShippingLabelModel(
            labelId = labelEntity.labelId,
            tracking = labelEntity.tracking,
            refundableAmount = labelEntity.refundableAmount,
            status = ShippingLabelStatus.valueOf(labelEntity.status),
            created = labelEntity.created,
            carrierId = labelEntity.carrierId,
            serviceName = labelEntity.serviceName,
            commercialInvoiceUrl = labelEntity.commercialInvoiceUrl,
            isCommercialInvoiceSubmittedElectronically = labelEntity.isCommercialInvoiceSubmittedElectronically,
            packageName = labelEntity.packageName,
            isLetter = labelEntity.isLetter,
            productNames = labelEntity.productNames,
            productIds = labelEntity.productIds,
            shipmentId = labelEntity.shipmentId,
            receiptItemId = labelEntity.receiptItemId,
            createdDate = labelEntity.createdDate,
            mainReceiptId = labelEntity.mainReceiptId,
            rate = labelEntity.rate,
            currency = labelEntity.currency,
            expiryDate = labelEntity.expiryDate,
            usedDate = labelEntity.usedDate,
            refund = labelEntity.refund?.let { refund ->
                ShippingLabelModel.Refund(status = refund.status, requestDate = refund.requestDate)
            },
            hazmatCategory = labelEntity.hazmatCategory?.takeUnless { it.isEmpty() }?.let {
                runCatching { ShippingLabelHazmatCategory.valueOf(it) }.getOrNull()
            },
            originAddress = labelEntity.originAddress?.let { invoke(it) },
            destinationAddress = labelEntity.destinationAddress?.let { invoke(it) }
        )
    }

    operator fun invoke(entityAddress: WooShippingLabelEntity.Address): Address {
        val (firstName, lastName) = parseFullName(entityAddress.name)
        return Address(
            company = entityAddress.company.orEmpty(),
            firstName = firstName,
            lastName = lastName,
            phone = entityAddress.phone.orEmpty(),
            country = Location(
                name = entityAddress.countryCode.orEmpty(),
                code = entityAddress.countryCode.orEmpty()
            ),
            state = AmbiguousLocation.Raw(entityAddress.state.orEmpty()),
            address1 = entityAddress.address1.orEmpty(),
            address2 = entityAddress.address2.orEmpty(),
            city = entityAddress.city.orEmpty(),
            postcode = entityAddress.postcode.orEmpty(),
            email = ""
        )
    }

    operator fun invoke(destinationAddressDTO: DestinationAddressDTO): Address {
        val (firstName, lastName) = parseFullName(destinationAddressDTO.name)
        return Address(
            company = destinationAddressDTO.company.orEmpty(),
            address1 = destinationAddressDTO.address.orEmpty(),
            city = destinationAddressDTO.city.orEmpty(),
            state = AmbiguousLocation.Raw(destinationAddressDTO.state.orEmpty()),
            postcode = destinationAddressDTO.postcode.orEmpty(),
            country = Location(
                name = destinationAddressDTO.country.orEmpty(),
                code = destinationAddressDTO.country.orEmpty()
            ),
            firstName = firstName,
            phone = destinationAddressDTO.phone.orEmpty(),
            address2 = destinationAddressDTO.address2.orEmpty(),
            email = "", // We set the email later from the order details
            lastName = lastName
        )
    }

    operator fun invoke(originAddressPurchaseDTO: OriginAddressPurchaseDTO): OriginShippingAddress {
        val (firstName, lastName) = parseFullName(originAddressPurchaseDTO.name)
        return OriginShippingAddress(
            id = originAddressPurchaseDTO.id.orEmpty(),
            address1 = originAddressPurchaseDTO.address,
            address2 = originAddressPurchaseDTO.address2,
            city = originAddressPurchaseDTO.city,
            state = originAddressPurchaseDTO.state,
            postcode = originAddressPurchaseDTO.postcode.orEmpty(),
            country = originAddressPurchaseDTO.country.orEmpty(),
            firstName = firstName,
            company = originAddressPurchaseDTO.company,
            phone = originAddressPurchaseDTO.phone.orEmpty(),
            email = originAddressPurchaseDTO.email.orEmpty(),
            isDefault = false,
            isVerified = originAddressPurchaseDTO.isVerified,
            lastName = lastName
        )
    }

    operator fun invoke(purchasedShippingLabelResponseDTO: PurchasedShippingLabelResponseDTO) = PurchasedLabelData(
        labels = purchasedShippingLabelResponseDTO.labels.map { invoke(it) },
        destination = purchasedShippingLabelResponseDTO.selectedDestination.mapValues { invoke(it.value) },
        origin = purchasedShippingLabelResponseDTO.selectedOrigin.mapValues { invoke(it.value) },
        rates = purchasedShippingLabelResponseDTO.selectedRates.mapValues {
            requireNotNull(ratesMapper(it.key, it.value))
        }
    )

    operator fun invoke(addressListDTO: Array<AddressDTO>): List<OriginShippingAddress> {
        return addressListDTO.map {
            OriginShippingAddress(
                id = it.id.orEmpty(),
                address1 = it.address.orEmpty(),
                address2 = it.address2.orEmpty(),
                city = it.city.orEmpty(),
                state = it.state.orEmpty(),
                postcode = it.postcode.orEmpty(),
                country = it.country.orEmpty(),
                firstName = it.firstName.orEmpty(),
                lastName = it.lastName.orEmpty(),
                company = it.company.orEmpty(),
                phone = it.phone.orEmpty(),
                email = it.email.orEmpty(),
                isDefault = it.defaultAddress,
                isVerified = it.isVerified,
            )
        }
    }

    operator fun invoke(addressDTO: AddressDTO): Address {
        return Address(
            address1 = addressDTO.address.orEmpty(),
            address2 = addressDTO.address2.orEmpty(),
            city = addressDTO.city.orEmpty(),
            state = AmbiguousLocation.Raw(addressDTO.state.orEmpty()),
            postcode = addressDTO.postcode.orEmpty(),
            country = AmbiguousLocation.Raw(addressDTO.country.orEmpty()).asLocation(),
            firstName = if (addressDTO.name.isNullOrEmpty()) addressDTO.firstName.orEmpty() else addressDTO.name,
            lastName = addressDTO.lastName.orEmpty(),
            company = addressDTO.company.orEmpty(),
            phone = addressDTO.phone.orEmpty(),
            email = addressDTO.email.orEmpty(),
        )
    }

    fun toOriginAddress(addressDTO: AddressDTO): OriginShippingAddress {
        return OriginShippingAddress(
            id = addressDTO.id.orEmpty(),
            address1 = addressDTO.address.orEmpty(),
            address2 = addressDTO.address2.orEmpty(),
            city = addressDTO.city.orEmpty(),
            state = addressDTO.state.orEmpty(),
            postcode = addressDTO.postcode.orEmpty(),
            country = addressDTO.country.orEmpty(),
            firstName = addressDTO.firstName.orEmpty(),
            lastName = addressDTO.lastName.orEmpty(),
            company = addressDTO.company.orEmpty(),
            phone = addressDTO.phone.orEmpty(),
            email = addressDTO.email.orEmpty(),
            isDefault = addressDTO.defaultAddress,
            isVerified = addressDTO.isVerified,
        )
    }

    operator fun invoke(normalizationResponseDTO: NormalizationResponseDTO): AddressNormalizationModel {
        return AddressNormalizationModel(
            address = invoke(normalizationResponseDTO.address),
            normalizedAddress = normalizationResponseDTO.normalizedAddress?.let {
                invoke(normalizationResponseDTO.normalizedAddress)
            } ?: Address.EMPTY,
            isTrivial = normalizationResponseDTO.isTrivialNormalization,
            errors = normalizationResponseDTO.errors
        )
    }

    operator fun invoke(
        shipments: ShipmentMap,
        site: SiteModel,
        orderId: Long,
    ): List<WooShippingShipmentEntity> {
        return shipments.map { (shipmentId, items) ->
            WooShippingShipmentEntity(
                localSiteId = site.localId(),
                orderId = LocalOrRemoteId.RemoteId(orderId),
                shipmentId = shipmentId,
                items = items.map { item ->
                    WooShippingShipmentEntity.Item(
                        id = item.id,
                        subItems = item.subItems
                    )
                }
            )
        }
    }

    operator fun invoke(shipmentEntities: List<WooShippingShipmentEntity>): ShipmentMap {
        return shipmentEntities.associate { entity ->
            val items = entity.items.map {
                ShipmentItem(
                    id = it.id,
                    subItems = it.subItems
                )
            }
            entity.shipmentId to items
        }
    }

    fun toOriginAddressPurchaseDTO(address: OriginShippingAddress): OriginAddressPurchaseDTO {
        return OriginAddressPurchaseDTO(
            id = address.id,
            address = address.address1,
            address2 = address.address2,
            city = address.city,
            state = address.state,
            postcode = address.postcode,
            country = address.country,
            name = combineStrings(address.firstName.orEmpty(), address.lastName.orEmpty())
                .takeIf { it.isNotEmpty() },
            company = address.company,
            phone = address.phone,
            email = address.email
        )
    }

    fun toDestinationAddressDTO(address: Address): DestinationAddressDTO {
        return DestinationAddressDTO(
            company = address.company,
            name = combineStrings(address.firstName, address.lastName).takeIf { it.isNotEmpty() },
            phone = address.phone,
            address = address.address1,
            address2 = address.address2,
            city = address.city,
            state = address.state.codeOrRaw,
            postcode = address.postcode,
            country = address.country.code,
        )
    }

    fun toPackagePurchaseDTO(
        shipmentId: Int,
        selectedPackage: PackageData,
        selectedRate: WooShippingSelectedRateModel,
        shippableItems: List<Long>,
        weight: Float
    ): PackagePurchaseDTO {
        return PackagePurchaseDTO(
            id = shipmentId.toString(),
            boxId = selectedPackage.id,
            length = selectedPackage.length.toFloat(),
            width = selectedPackage.width.toFloat(),
            height = selectedPackage.safeHeight.toFloat(),
            weight = weight,
            isLetter = selectedPackage.isLetter,
            shipmentId = selectedRate.rate.shipmentId,
            products = shippableItems,
            rateId = selectedRate.rate.rateId,
            serviceId = selectedRate.rate.serviceId,
            carrierId = selectedRate.rate.carrierId,
            serviceName = selectedRate.rate.serviceName,
            signature = when (selectedRate.rate.option) {
                WooShippingRateModel.Option.SIGNATURE -> "yes"
                WooShippingRateModel.Option.ADULT_SIGNATURE -> "adult"
                else -> null
            },
            carbonNeutral = selectedRate.additionalRates
                .any { it.option == WooShippingRateModel.Option.CARBON_NEUTRAL }
                .takeIf { true },
            additionalHandling = selectedRate.additionalRates
                .any { it.option == WooShippingRateModel.Option.ADDITIONAL_HANDLING }
                .takeIf { true },
            saturdayDelivery = selectedRate.additionalRates
                .any { it.option == WooShippingRateModel.Option.SATURDAY_DELIVERY }
                .takeIf { true }
        )
    }

    fun toRateDTO(selectedRate: WooShippingRateModel): RateDTO {
        return RateDTO(
            rateId = selectedRate.rateId,
            serviceId = selectedRate.serviceId,
            carrierId = selectedRate.carrierId,
            title = selectedRate.serviceName,
            rate = selectedRate.price,
            deliveryDays = selectedRate.deliveryDays,
            shipmentId = selectedRate.shipmentId,
            deliveryDate = selectedRate.deliveryDate,
            deliveryDateGuaranteed = selectedRate.isDeliveryDateGuaranteed,
            freePickup = selectedRate.hasFreePickup,
            insurance = selectedRate.insurance,
            isSelected = selectedRate.isSelected,
            tracking = selectedRate.isTrackingEnabled,
            listRate = selectedRate.listRate,
            retailRate = selectedRate.retailRate,
            type = when (selectedRate.option) {
                WooShippingRateModel.Option.SIGNATURE, WooShippingRateModel.Option.ADULT_SIGNATURE ->
                    selectedRate.option.id.snakeToCamelCase()

                else -> null
            }
        )
    }

    fun toSelectedRateOptions(
        selectedRate: WooShippingSelectedRateModel
    ): Map<WooShippingRateModel.Option, ShippingRateSurchargeDTO> {
        val additionalRates = selectedRate.additionalRates.associate {
            it.option to ShippingRateSurchargeDTO(
                value = true,
                surcharge = selectedRate.getSurcharge(it.option)
            )
        }

        val signatureSurcharge = if (selectedRate.rate.option == WooShippingRateModel.Option.SIGNATURE ||
            selectedRate.rate.option == WooShippingRateModel.Option.ADULT_SIGNATURE
        ) {
            selectedRate.rate.option to ShippingRateSurchargeDTO(
                value = if (selectedRate.rate.option == WooShippingRateModel.Option.SIGNATURE) {
                    "yes"
                } else {
                    "adult"
                },
                surcharge = selectedRate.getSurcharge(selectedRate.rate.option)
            )
        } else {
            null
        }
        return additionalRates + (signatureSurcharge?.let { mapOf(it) } ?: emptyMap())
    }

    fun toAddressDTO(address: Address, id: String? = null, isVerified: Boolean = false): AddressDTO {
        return AddressDTO(
            id = id,
            address = address.address1,
            address2 = address.address2,
            city = address.city,
            state = address.state.codeOrRaw,
            postcode = address.postcode,
            country = address.country.code,
            company = address.company,
            name = combineStrings(address.firstName, address.lastName),
            phone = address.phone,
            email = address.email,
            isVerified = isVerified
        )
    }

    fun toCustomsDTO(
        customsData: CustomsData
    ): CustomsDTO {
        return CustomsDTO(
            contentsType = customsData.contentType.name.toLowerCase(Locale.current),
            contentExplanation = customsData.contentDescription,
            restrictionType = customsData.restrictionType.name.toLowerCase(Locale.current),
            restrictionComments = customsData.restrictionDescription,
            isReturnToSender = if (customsData.isReturnToSender) "return" else "abandon",
            itn = customsData.itn,
            items = customsData.items.map {
                CustomsItemDTO(
                    productId = it.productID,
                    description = it.description,
                    quantity = it.quantity,
                    value = it.value.toDouble(),
                    weight = it.weight.toDouble(),
                    hsTariffNumber = it.hsTariffNumber.replace(Regex("""\D"""), ""),
                    originCountry = it.originCountryCode
                )
            }
        )
    }

    fun toHazmatDTO(hazmatSelection: ShippingLabelHazmatCategory?) =
        hazmatSelection?.let {
            HazmatDTO(
                isHazmat = true,
                category = hazmatSelection.requestFieldValue
            )
        } ?: HazmatDTO()

    private fun parseFullName(name: String?): Pair<String, String> {
        val safeName = name.orEmpty()
        val lastSpaceIndex = safeName.indexOfLast { it == ' ' }
        return if (lastSpaceIndex == -1) {
            "" to ""
        } else {
            safeName.substring(0, lastSpaceIndex) to safeName.substring(lastSpaceIndex + 1)
        }
    }
}
