package com.woocommerce.android.ui.orders

import com.google.gson.Gson
import com.woocommerce.android.helpers.WcDateTimeTestUtils
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT_SPACE
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.RIGHT
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.RIGHT_SPACE
import org.wordpress.android.fluxc.utils.WCCurrencyUtils
import org.wordpress.android.util.LanguageUtils
import kotlin.math.absoluteValue

object WcOrderTestUtils {
    /**
     * [WCOrderModel.LineItem] values cannot be modified so adding as string here
     */
    private val MULTIPLE_PRODUCTS = Gson().toJson(
            listOf(
                    mapOf(
                            "productId" to "290",
                            "variationId" to "0",
                            "name" to "Black T-shirt",
                            "quantity" to 1,
                            "subtotal" to 10
                    ),
                    mapOf(
                            "productId" to "291",
                            "variationId" to "2",
                            "name" to "White Pants",
                            "quantity" to 2,
                            "subtotal" to 12
                    )
            )
    )

    /**
     * Generates an array containing multiple [WCOrderModel] objects.
     */
    fun generateOrders(): List<WCOrderModel> {
        val result = ArrayList<WCOrderModel>()
        val om1 = WCOrderModel(1).apply {
            // Empty first/last name
            billingFirstName = ""
            billingLastName = ""
            // Currency : USD
            currency = "USD"
            // today
            dateCreated = WcDateTimeTestUtils.formatDate(WcDateTimeTestUtils.getCurrentDateTime())
            localSiteId = 1
            number = "1"
            // Processing
            status = "Processing"
            total = "14.53"
        }

        val om2 = WCOrderModel(2).apply {
            // really long first & last name
            billingFirstName = "Itsareallylongnametoseehowitishandled"
            billingLastName = "andcontinuingwiththelongname"
            // Currency : CAD
            currency = "CAD"
            // Yesterday
            dateCreated = WcDateTimeTestUtils.formatDate(WcDateTimeTestUtils.getCurrentDateTimeMinusDays(1))
            localSiteId = 1
            number = "63"
            // Pending payment
            status = "Pending"
            total = "14.53"
        }

        val om3 = WCOrderModel(3).apply {
            billingFirstName = "Mandy"
            billingLastName = "Sykes"
            // Currency : EURO
            currency = "EUR"
            // 2 days ago
            dateCreated = WcDateTimeTestUtils.formatDate(WcDateTimeTestUtils.getCurrentDateTimeMinusDays(3))
            localSiteId = 1
            number = "14"
            // On Hold
            status = "On Hold"
            total = "14.53"
        }

        val om4 = WCOrderModel(4).apply {
            billingFirstName = "Jennifer"
            billingLastName = "Johnson"
            // Currency : INR
            currency = "INR"
            // More than a week
            dateCreated = WcDateTimeTestUtils.formatDate(WcDateTimeTestUtils.getCurrentDateTimeMinusDays(14))
            localSiteId = 1
            number = "15"
            // Completed
            status = "Completed"
            total = "14.53"
        }

        val om5 = WCOrderModel(5).apply {
            billingFirstName = "Christopher"
            billingLastName = "Jones"
            currency = "AUD"
            // Older than a month
            dateCreated = WcDateTimeTestUtils.formatDate(WcDateTimeTestUtils.getCurrentDateTimeMinusMonths(2))
            localSiteId = 1
            number = "3"
            // Cancelled
            status = "Cancelled"
            total = "14.53"
        }

        val om6 = WCOrderModel(6).apply {
            billingFirstName = "Carissa"
            billingLastName = "King"
            currency = "USD"
            dateCreated = "2018-02-02T16:11:13Z"
            localSiteId = 1
            number = "55"
            // Refunded
            status = "Refunded"
            total = "14.53"
        }

        val om7 = WCOrderModel(7).apply {
            billingFirstName = "Carissa"
            billingLastName = "King"
            currency = "USD"
            dateCreated = "2018-02-02T16:11:13Z"
            localSiteId = 1
            number = "55"
            // Failed
            status = "Failed"
            total = "14.53"
        }

        result.add(om1)
        result.add(om2)
        result.add(om3)
        result.add(om4)
        result.add(om5)
        result.add(om6)
        result.add(om7)

        return result
    }

    /**
     * Generates a single [WCOrderModel] object for Order detail screen.
     */
    fun generateOrderDetail(
        dateCreatedString: String = "",
        note: String = "",
        billingAddress1: String = "",
        billingCountry: String = "",
        billingPostalCode: String = "",
        shippingFirstName: String = "",
        shippingLastName: String = "",
        shippingAddress1: String = "",
        shippingCountry: String = "",
        billingPhone: String = "",
        products: String = MULTIPLE_PRODUCTS,
        orderStatus: String = "completed",
        shippingTotal: String = "12",
        totalTax: String = "2",
        total: String = "44",
        discountTotal: String = "",
        discountCodes: String = "",
        paymentMethodTitle: String = "",
        currency: String = "USD",
        refundTotal: Double = 0.00
    ): WCOrderModel {
        return WCOrderModel(2).apply {
            billingFirstName = "Jane"
            billingLastName = "Masterson"
            this.billingAddress1 = billingAddress1
            this.billingCountry = billingCountry
            this.currency = currency
            dateCreated = dateCreatedString
            localSiteId = 1
            this.shippingFirstName = shippingFirstName
            this.shippingLastName = shippingLastName
            this.shippingAddress1 = shippingAddress1
            this.shippingCountry = shippingCountry
            this.billingPostcode = billingPostalCode
            billingEmail = "test@testing.com"
            this.billingPhone = billingPhone
            number = "1"
            status = "pending"
            customerNote = note
            lineItems = products
            status = orderStatus
            this.shippingTotal = shippingTotal
            this.totalTax = totalTax
            this.total = total
            this.discountCodes = discountCodes
            this.discountTotal = discountTotal
            this.paymentMethodTitle = paymentMethodTitle
            this.refundTotal = refundTotal
        }
    }

    /**
     * Generates a single [WCOrderModel] object for Order detail screen.
     */
    fun generateOrderStatusDetail(status: String = "Pending"): WCOrderStatusModel {
        return WCOrderStatusModel(1).apply {
            label = status
            statusKey = status
        }
    }

    /**
     * Generates an array containing multiple [WCOrderNoteModel] objects.
     */
    fun generateSampleNotes(): List<WCOrderNoteModel> {
        val siteId = 1
        val orderId = 1
        val remoteId: Long = 1
        val result = ArrayList<WCOrderNoteModel>()
        val om1 = WCOrderNoteModel().apply {
            localSiteId = siteId
            localOrderId = orderId
            remoteNoteId = remoteId
            dateCreated = "2019-04-05T17:12:00Z"
            note = "This should be displayed first"
            isCustomerNote = true
            isSystemNote = false
        }

        val om2 = WCOrderNoteModel().apply {
            localSiteId = siteId
            localOrderId = orderId
            remoteNoteId = remoteId
            dateCreated = "2018-11-05T14:15:00Z"
            note = "This should be displayed second"
            isCustomerNote = false
            isSystemNote = true
        }

        val om3 = WCOrderNoteModel().apply {
            localSiteId = siteId
            localOrderId = orderId
            remoteNoteId = remoteId
            dateCreated = "2016-12-04T12:15:00Z"
            note = "This should be displayed third"
            isSystemNote = false
            isCustomerNote = false
        }

        result.add(om1)
        result.add(om2)
        result.add(om3)

        return result
    }

    /**
     * Generates an map containing multiple [WCOrderStatusModel] objects.
     */
    fun generateOrderStatusOptions(): Map<String, WCOrderStatusModel> {
        val options = listOf(
                WCOrderStatusModel(id = 1).apply {
                    label = "Pending Payment"
                    statusKey = "pending"
                },
                WCOrderStatusModel(id = 2).apply {
                    label = "Processing"
                    statusKey = "processing"
                },
                WCOrderStatusModel(id = 3).apply {
                    label = "On Hold"
                    statusKey = "on-hold"
                },
                WCOrderStatusModel(id = 4).apply {
                    label = "Completed"
                    statusKey = "completed"
                },
                WCOrderStatusModel(id = 5).apply {
                    label = "Cancelled"
                    statusKey = "cancelled"
                },
                WCOrderStatusModel(id = 6).apply {
                    label = "Refunded"
                    statusKey = "refunded"
                },
                WCOrderStatusModel(id = 7).apply {
                    label = "Failed"
                    statusKey = "failed"
                }
        )
        return options.map { it.statusKey to it }.toMap()
    }

    fun generateOrderShipmentTrackings(): List<WCOrderShipmentTrackingModel> {
        val result = ArrayList<WCOrderShipmentTrackingModel>()

        val om1 = WCOrderShipmentTrackingModel().apply {
            localSiteId = 1
            trackingProvider = "Anitaa Test"
            trackingNumber = "1111-1111-1111-1111"
            trackingLink = "http://somesite.com"
            dateShipped = "2018-02-02"
        }

        val om2 = WCOrderShipmentTrackingModel().apply {
            localSiteId = 1
            trackingProvider = "DHL"
            trackingNumber = "2222-2222-2222-2222"
            dateShipped = "2019-01-01"
        }

        val om3 = WCOrderShipmentTrackingModel().apply {
            localSiteId = 1
            trackingProvider = "Fedex"
            trackingNumber = "3333-3333-3333-3333"
            trackingLink = "http://testlink3.com"
            dateShipped = "2019-02-28"
        }

        val om4 = WCOrderShipmentTrackingModel().apply {
            localSiteId = 1
            trackingProvider = "Axle"
            trackingNumber = "4444"
            trackingLink = "http://testlink4.com"
            dateShipped = "2019-05-23"
        }

        result.add(om1)
        result.add(om2)
        result.add(om3)
        result.add(om4)
        return result
    }

    /**
     * Generates a single [WCSettingsModel] object for a mock Site
     */
    fun generateSiteSettings(
        localSiteId: Int = 1,
        currencyCode: String = "USD",
        currencyPosition: CurrencyPosition = CurrencyPosition.LEFT
    ): WCSettingsModel {
        return WCSettingsModel(
                localSiteId = localSiteId,
                currencyCode = currencyCode,
                currencyPosition = currencyPosition,
                currencyThousandSeparator = ",",
                currencyDecimalSeparator = ".",
                currencyDecimalNumber = 2)
    }

    /**
     * Mock of the method in FluxC
     * Not sure if this is the best way to mock SiteSettings in FluxC.
     *
     * So the issue is, formatting currency for display, depends on the store's settings.
     * If the store settings is null, then the currency by default is displayed with only 1 decimal place,
     * or in some cases no decimal places. But default behaviour is to display currency code to the left
     * with 2 decimal places.
     * In order to correctly validate the currency formatting, we need to mock the `getSiteSettings` method
     * inside `WooCommerceStore` class. This method is called directly from FluxC when trying to format
     * currency.
     *
     * The `WooCommerceStore` is final so we cannot mock/spy the class.
     * So a simple
     * doReturn(WcOrderTestUtils.generateSiteSettings()).whenever(mockWcStore).getSiteSettings(any())
     * will result in cannot mock/spy the class error.
     *
     * This approach was to mock the CurrencyFormatter class. But the only logic in this class is to call
     * the appropriate method in FluxC. So rather than calling this method in CurrencyFormatter class,
     * we can mock this method to return the appropriate value. But the logic to format is still retained in
     * fluxC so we would need to replicate this logic in this method and check if the value matches.
     * I admit I am not sure if this is the best approach since we would only be replicating the same logic
     * from FluxC into Woo and that too only for UI testing purposes.
     * If in future the logic is changed in FluxC, the UI tests would fail and we would need to replicate this
     * inside Woo UI test as well.
     *
     * Another approach would be to send the SiteSettingsModel to the format currency method in `WooCommerceStore`.
     * This way we can mock the method to fetch the SiteSettings in Woo itself and then pass the value to
     * FluxC. This would mean making modifications to FluxC and I don't see this as really useful for anything
     * other than fixing this issue since we would be fetching the SiteSettingsModel from FluxC and then
     * passing the same to another method in FluxC instead of keeping the entire logic inside a single method.
     *
     * I ran out of ideas at this point other than to just insert a mock WcSiteSettingsModel directly into the local db
     * when UI tests first start, which seems like a bad idea!
     */
    fun formatCurrencyForDisplay(
        rawValue: String,
        siteSettings: WCSettingsModel?,
        currencyCode: String? = null,
        applyDecimalFormatting: Boolean
    ): String {
        // Resolve the currency code to a localized symbol
        val resolvedCurrencyCode = currencyCode ?: siteSettings?.currencyCode
        val currencySymbol = resolvedCurrencyCode?.let {
            WCCurrencyUtils.getLocalizedCurrencySymbolForCode(it, LanguageUtils.getCurrentDeviceLanguage())
        } ?: ""

        // Format the amount for display according to the site's currency settings
        // Use absolute values - if the value is negative, it will be handled in the next step, with the currency symbol
        val decimalFormattedValue = siteSettings?.takeIf { applyDecimalFormatting }?.let {
            WCCurrencyUtils.formatCurrencyForDisplay(rawValue.toDoubleOrNull()?.absoluteValue ?: 0.0, it)
        } ?: rawValue.removePrefix("-")

        // Append or prepend the currency symbol according to the site's settings
        with(StringBuilder()) {
            if (rawValue.startsWith("-")) { append("-") }
            append(when (siteSettings?.currencyPosition) {
                null, LEFT -> "$currencySymbol$decimalFormattedValue"
                LEFT_SPACE -> "$currencySymbol $decimalFormattedValue"
                RIGHT -> "$decimalFormattedValue$currencySymbol"
                RIGHT_SPACE -> "$decimalFormattedValue $currencySymbol"
            })
            return toString()
        }
    }
}
