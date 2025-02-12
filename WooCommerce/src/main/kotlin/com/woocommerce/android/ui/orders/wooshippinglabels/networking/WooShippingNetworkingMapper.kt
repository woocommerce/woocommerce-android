package com.woocommerce.android.ui.orders.wooshippinglabels.networking

import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AddressNormalizationModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PurchasedLabelData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRatesDatasourceMapper
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.DestinationAddressDTO
import com.woocommerce.android.util.StringUtils.combineStrings
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

class WooShippingNetworkingMapper @Inject constructor(
    private val ratesMapper: WooShippingRatesDatasourceMapper
) {
    operator fun invoke(storeOptionsDTO: StoreOptionsDTO): StoreOptionsModel {
        return StoreOptionsModel(
            currencySymbol = storeOptionsDTO.currencySymbol.orEmpty(),
            dimensionUnit = storeOptionsDTO.dimensionUnit.orEmpty(),
            weightUnit = storeOptionsDTO.weightUnit.orEmpty(),
            originCountry = storeOptionsDTO.originCountry.orEmpty()
        )
    }

    operator fun invoke(shippingLabelDTO: ShippingLabelDTO): ShippingLabelModel {
        return ShippingLabelModel(
            labelId = shippingLabelDTO.labelId ?: 0,
            tracking = shippingLabelDTO.tracking.orEmpty(),
            refundableAmount = shippingLabelDTO.refundableAmount ?: BigDecimal.ZERO,
            status = mapShippingLabelStatus(shippingLabelDTO.status),
            created = shippingLabelDTO.created?.let { Date(it) },
            carrierId = shippingLabelDTO.carrierId.orEmpty(),
            serviceName = shippingLabelDTO.serviceName.orEmpty(),
            commercialInvoiceUrl = shippingLabelDTO.commercialInvoiceUrl.orEmpty(),
            isCommercialInvoiceSubmittedElectronically =
            shippingLabelDTO.isCommercialInvoiceSubmittedElectronically ?: false,
            packageName = shippingLabelDTO.packageName.orEmpty(),
            isLetter = shippingLabelDTO.isLetter ?: false,
            productNames = shippingLabelDTO.productNames.orEmpty(),
            productIds = shippingLabelDTO.productIds.orEmpty(),
            receiptItemId = shippingLabelDTO.receiptItemId ?: 0,
            createdDate = shippingLabelDTO.createdDate?.let { Date(it) },
            mainReceiptId = shippingLabelDTO.mainReceiptId ?: 0,
            rate = shippingLabelDTO.rate ?: BigDecimal.ZERO,
            currency = shippingLabelDTO.currency.orEmpty(),
            expiryDate = shippingLabelDTO.expiryDate ?: 0
        )
    }

    operator fun invoke(destinationAddressDTO: DestinationAddressDTO): Address {
        val name = destinationAddressDTO.name?.split(" ") ?: listOf("", "")
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
            firstName = name.getOrElse(0) { "" },
            phone = destinationAddressDTO.phone.orEmpty(),
            address2 = destinationAddressDTO.address2.orEmpty(),
            email = destinationAddressDTO.email.orEmpty(),
            lastName = name.getOrElse(1) { "" }
        )
    }

    operator fun invoke(originAddressPurchaseDTO: OriginAddressPurchaseDTO): OriginShippingAddress {
        val name = originAddressPurchaseDTO.name?.split(" ") ?: listOf("", "")
        return OriginShippingAddress(
            id = originAddressPurchaseDTO.id.orEmpty(),
            address1 = originAddressPurchaseDTO.address,
            address2 = originAddressPurchaseDTO.address2,
            city = originAddressPurchaseDTO.city,
            state = originAddressPurchaseDTO.state,
            postcode = originAddressPurchaseDTO.postcode.orEmpty(),
            country = originAddressPurchaseDTO.country.orEmpty(),
            firstName = name.getOrElse(0) { "" },
            company = originAddressPurchaseDTO.company,
            phone = originAddressPurchaseDTO.phone.orEmpty(),
            email = originAddressPurchaseDTO.email.orEmpty(),
            isDefault = false,
            isVerified = originAddressPurchaseDTO.isVerified,
            lastName = name.getOrElse(1) { "" }
        )
    }

    operator fun invoke(purchasedShippingLabelResponseDTO: PurchasedShippingLabelResponseDTO): PurchasedLabelData {
        return PurchasedLabelData(
            labels = purchasedShippingLabelResponseDTO.labels.map { invoke(it) },
            destination = purchasedShippingLabelResponseDTO.selectedDestination.mapValues { invoke(it.value) },
            origin = purchasedShippingLabelResponseDTO.selectedOrigin.mapValues { invoke(it.value) },
            rates = purchasedShippingLabelResponseDTO.selectedRates.mapValues { ratesMapper(it.key, it.value) }
        )
    }

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
            firstName = if(addressDTO.name.isNullOrEmpty()) addressDTO.firstName.orEmpty() else addressDTO.name,
            lastName = addressDTO.lastName.orEmpty(),
            company = addressDTO.company.orEmpty(),
            phone = addressDTO.phone.orEmpty(),
            email = addressDTO.email.orEmpty(),
        )
    }

    operator fun invoke(normalizationResponseDTO: NormalizationResponseDTO): AddressNormalizationModel {
        return AddressNormalizationModel(
            address = invoke(normalizationResponseDTO.address),
            normalizedAddress = invoke(normalizationResponseDTO.normalizedAddress),
            isTrivial = normalizationResponseDTO.isTrivialNormalization
        )
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
            name = "${address.firstName} ${address.lastName}",
            company = address.company,
            phone = address.phone
        )
    }

    fun toDestinationAddressDTO(address: Address): DestinationAddressDTO {
        return DestinationAddressDTO(
            address = address.address1,
            city = address.city,
            state = address.state.codeOrRaw,
            postcode = address.postcode,
            country = address.country.code,
            name = "${address.firstName} ${address.lastName}"
        )
    }

    fun toPackagePurchaseDTO(
        selectedPackage: PackageData,
        selectedRate: WooShippingRateModel,
        shippableItems: List<Long>,
        weight: Float
    ): PackagePurchaseDTO {
        return PackagePurchaseDTO(
            id = selectedPackage.id,
            boxId = "default_package",
            length = selectedPackage.length.toFloat(),
            width = selectedPackage.width.toFloat(),
            height = selectedPackage.height.toFloat(),
            weight = weight,
            isLetter = selectedPackage.isLetter,
            shipmentId = selectedRate.shipmentId,
            products = shippableItems,
            rateId = selectedRate.rateId,
            serviceId = selectedRate.serviceId,
            carrierId = selectedRate.carrierId,
            serviceName = selectedRate.serviceName
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
            retailRate = selectedRate.discount
        )
    }

    private fun mapShippingLabelStatus(status: String?): ShippingLabelStatus {
        return when (status) {
            PURCHASE_IN_PROGRESS_KEY -> ShippingLabelStatus.PurchaseInProgress
            PURCHASED_KEY -> ShippingLabelStatus.Purchased
            PURCHASE_ERROR_KEY -> ShippingLabelStatus.PurchaseError
            ANONYMIZED_KEY -> ShippingLabelStatus.Anonymized
            else -> ShippingLabelStatus.Unknown
        }
    }

    fun toAddressDTO(address: Address): AddressDTO {
        return AddressDTO(
            address = address.address1,
            address2 = address.address2,
            city = address.city,
            state = address.state.codeOrRaw,
            postcode = address.postcode,
            country = address.country.code,
            company = address.company,
            name = combineStrings(address.firstName, address.lastName),
            phone = address.phone,
            email = address.email
        )
    }

    companion object {
        private const val PURCHASE_IN_PROGRESS_KEY = "PURCHASE_IN_PROGRESS"
        private const val PURCHASED_KEY = "PURCHASED"
        private const val PURCHASE_ERROR_KEY = "PURCHASE_ERROR"
        private const val ANONYMIZED_KEY = "ANONYMIZED"
    }
}
