package com.woocommerce.android.ui.products.addons

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductTaxStatus
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import java.math.BigDecimal
import java.util.Date

object AddonTestFixtures {
    private val defaultAddress
        get() = Address(
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "")

    val defaultOrder
        get() = Order(
            identifier = OrderIdentifier(0, 0),
            remoteId = 0,
            number = "",
            localSiteId = 0,
            dateCreated = Date(),
            dateModified = Date(),
            datePaid = null,
            status = Order.Status.Pending,
            total = BigDecimal(0),
            productsTotal = BigDecimal(0),
            totalTax = BigDecimal(0),
            shippingTotal = BigDecimal(0),
            discountTotal = BigDecimal(0),
            refundTotal = BigDecimal(0),
            feesTotal = BigDecimal(0),
            currency = "",
            customerNote = "",
            discountCodes = "",
            paymentMethod = "",
            paymentMethodTitle = "",
            isCashPayment = false,
            pricesIncludeTax = false,
            multiShippingLinesAvailable = false,
            billingAddress = defaultAddress,
            shippingAddress = defaultAddress,
            shippingMethods = listOf(),
            items = listOf(),
            shippingLines = listOf(),
            metaData = listOf()
        )

    val defaultOrderItem
        get() = Order.Item(
            itemId = 0,
            productId = 0,
            name = "",
            price = BigDecimal(0),
            sku = "",
            quantity = 0f,
            subtotal = BigDecimal(0),
            totalTax = BigDecimal(0),
            total = BigDecimal(0),
            variationId = 0,
            attributesList = listOf()
        )

    val defaultProduct
        get() = Product(
            remoteId = 0,
            name = "",
            description = "",
            shortDescription = "",
            type = "",
            status = null,
            catalogVisibility = null,
            isFeatured = false,
            stockStatus = ProductStockStatus.InStock,
            backorderStatus = ProductBackorderStatus.Yes,
            dateCreated = Date(),
            firstImageUrl = "",
            totalSales = 0,
            reviewsAllowed = false,
            isVirtual = false,
            ratingCount = 0,
            averageRating = 0f,
            permalink = "",
            externalUrl = "",
            buttonText = "",
            salePrice = null,
            regularPrice = null,
            taxClass = "",
            isStockManaged = false,
            stockQuantity = 0.0,
            sku = "",
            slug = "",
            length = 0f,
            width = 0f,
            height = 0f,
            weight = 0f,
            shippingClass = "",
            shippingClassId = 0,
            isDownloadable = false,
            downloads = listOf(),
            downloadLimit = 0,
            downloadExpiry = 0,
            purchaseNote = "",
            numVariations = 0,
            images = listOf(),
            attributes = listOf(),
            saleEndDateGmt = null,
            saleStartDateGmt = null,
            isSoldIndividually = false,
            taxStatus = ProductTaxStatus.Taxable,
            isSaleScheduled = false,
            menuOrder = 0,
            categories = listOf(),
            tags = listOf(),
            groupedProductIds = listOf(),
            crossSellProductIds = listOf(),
            upsellProductIds = listOf(),
            addons = listOf()
        )

    val defaultProductAddon
        get() = ProductAddon(
            name = "",
            description = "",
            descriptionEnabled = false,
            max = "",
            min = "",
            position = "",
            rawPrice = "",
            adjustPrice = "",
            required = false,
            restrictions = "",
            titleFormat = null,
            restrictionsType = null,
            priceType = null,
            type = null,
            display = null,
            rawOptions = listOf()
        )
}
