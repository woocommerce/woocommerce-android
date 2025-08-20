package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.ShippingRatesState
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodOptions
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingLabelPaperSize
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.CarrierUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateOptionUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingSortOption
import java.math.BigDecimal
import kotlin.random.Random

object ShippingLabelSampleData {
    fun getShipFrom() = OriginShippingAddress(
        firstName = "first name",
        lastName = "last name",
        company = "Company",
        phone = "",
        address1 = "A huge address that should be truncated",
        address2 = "",
        city = "City",
        postcode = "",
        email = "email",
        country = "US",
        state = "California",
        id = "id_1",
        isDefault = true,
        isVerified = true
    )

    fun getShipTo() = DestinationShippingAddress(
        address = Address(
            firstName = "first name",
            lastName = "last name",
            company = "Company",
            phone = "",
            address1 = "Another Address",
            address2 = "",
            city = "City",
            postcode = "",
            email = "email",
            country = Location("US", "USA"),
            state = AmbiguousLocation.Defined(Location("CA", "California", "USA")),
        ),
        isVerified = true
    )

    fun getShippingLines(number: Int = 3) = List(number) { i ->
        ShippingLineSummaryUI(
            title = "Shipping $i",
            amount = "$12.99"
        )
    }

    fun getPaymentsSection() = PaymentsSectionUI(
        selectedPaymentMethod = getPaymentMethod(),
        onEditPaymentMethodClicked = {}
    )

    fun getPurchaseSection() = PurchaseSectionUI(
        isVisible = true,
        isOrderAlreadyCompleted = false,
        markOrderComplete = true,
        formattedPrice = "$10.00",
        onPurchaseShippingLabel = {},
        onMarkOrderCompleteChange = {},
    )

    fun getPaymentOptions(countOfPaymentMethods: Int = 3) = PaymentMethodOptions(
        selectedPaymentId = if (countOfPaymentMethods > 0) 1 else null,
        paymentMethods = List(countOfPaymentMethods) { getPaymentMethod(it) },
        addPaymentMethodUrl = "https://example.com/add-payment-method",
        emailReceipts = true
    )

    fun getPaymentMethod(index: Int = 0) = PaymentMethodModel(
        paymentMethodId = index.toLong(),
        name = "John Doe",
        cardType = "VISA",
        cardDigits = "${index}234",
        expiry = "12/25"
    )

    fun getShippingRatesSection(): ShippingRatesState.DataState {
        return ShippingRatesState.DataState(
            selectedRatesSortOrder = ShippingSortOption.CHEAPEST,
            shippingRates = emptyMap(),
            selectedRate = null,
            onSelectedShippingRateChanged = {},
            onSelectedRateOptionChanged = { _, _ -> }
        )
    }

    fun generateShippingRates(): Map<CarrierUI, List<ShippingRateUI>> {
        val carriers = listOf(
            CarrierUI(
                carrier = WooShippingCarrier.DHL,
                name = "DHL Express",
                logoRes = R.drawable.dhl_logo
            ),
            CarrierUI(
                carrier = WooShippingCarrier.USPS,
                name = "USPS",
                logoRes = R.drawable.usps_logo
            ),
            CarrierUI(
                carrier = WooShippingCarrier.UPS,
                name = "UPS",
                logoRes = R.drawable.ups_logo
            ),
            CarrierUI(
                carrier = WooShippingCarrier.FEDEX,
                name = "Fed Ex",
                logoRes = R.drawable.fedex_logo
            ),
            CarrierUI(
                carrier = WooShippingCarrier.UNKNOWN,
                name = "Canada Post",
                logoRes = null
            )
        )

        return carriers.associateWith {
            generateRates(
                it.carrier,
                Random(0).nextInt(from = 3, until = 10)
            )
        }
    }

    fun generateRates(carrier: WooShippingCarrier, number: Int): List<ShippingRateUI> {
        return List(number) {
            val rate = WooShippingRateModel(
                packageId = "123$it",
                shipmentId = "123$it",
                rateId = "123$it",
                serviceId = "123$it",
                carrierId = "123$it",
                serviceName = "$carrier $it",
                deliveryDays = it,
                price = it.toBigDecimal(),
                option = WooShippingRateModel.Option.DEFAULT,
                carrier = WooShippingCarrier.DHL,
                hasFreePickup = true,
                insurance = BigDecimal.TEN,
                isTrackingEnabled = true,
                deliveryDate = null,
                isDeliveryDateGuaranteed = false,
                isSelected = false,
                listRate = BigDecimal.TEN,
                retailRate = BigDecimal.TEN
            )
            val option = ShippingRateOptionUI(
                option = rate.option,
                optionName = "",
                fee = BigDecimal.ZERO,
                formattedFee = "",
                rate = rate,
                feeDescription = "Default"
            )
            val options = mapOf(WooShippingRateModel.Option.DEFAULT to option)

            ShippingRateUI(
                title = rate.serviceName,
                formattedBasePrice = rate.price.toString(),
                options = options,
                shippingRateIncludedOptions = listOf(
                    "Tracking",
                    "Insurance",
                    "Free Pickup"
                ),
                formattedEstimatedDays = "$it business days",
                selectedOption = WooShippingRateModel.Option.DEFAULT,
                additionalSelectedOptions = emptyList()
            )
        }
    }

    fun getShippingRateSummaryUI(): ShipmentCostUI {
        return ShipmentCostUI(
            serviceName = "DHL Express",
            formattedBasePrice = "10.00",
            formattedTotalPrice = "12.00",
            optionsWithFees = mapOf(
                "Signature required" to "2.00"
            )
        )
    }

    fun getShipmentPrintLabelUI() = ShipmentPrintLabelUI(
        availablePrintSizes = listOf(WooShippingLabelPaperSize.LABEL, WooShippingLabelPaperSize.LETTER),
        isRefundAvailable = true,
        isCustomsFormAvailable = true
    )

    @Suppress("LongMethod")
    fun getShippingLabelUIModel(purchased: Boolean = false) = ShipmentUIModel(
        localId = "1",
        remoteId = "1",
        items = listOf(
            ShippableItemModel(
                itemId = 0,
                productId = 0,
                height = 0f,
                width = 0f,
                length = 0f,
                weight = 0f,
                title = "Sample Product",
                imageUrl = "",
                quantity = 1f,
                price = BigDecimal.TEN,
                currency = "USD"
            ),
            ShippableItemModel(
                itemId = 0,
                productId = 0,
                height = 0f,
                width = 0f,
                length = 0f,
                weight = 0f,
                title = "Sample Product 2",
                imageUrl = "",
                quantity = 2f,
                price = BigDecimal.TEN,
                currency = "USD"
            )
        ),
        label = if (purchased) {
            ShippingLabelModel(
                labelId = 0L,
                tracking = "TrackingNumber123",
                refundableAmount = BigDecimal.ZERO,
                status = ShippingLabelStatus.PURCHASED,
                created = null,
                carrierId = "",
                serviceName = "",
                commercialInvoiceUrl = "",
                isCommercialInvoiceSubmittedElectronically = false,
                packageName = "",
                isLetter = false,
                productNames = emptyList(),
                productIds = emptyList(),
                shipmentId = "shipment_1",
                receiptItemId = 0L,
                createdDate = null,
                mainReceiptId = 0L,
                rate = BigDecimal.ZERO,
                currency = "",
                expiryDate = 0L,
                usedDate = null,
                refund = null
            )
        } else {
            null
        }
    )
}
