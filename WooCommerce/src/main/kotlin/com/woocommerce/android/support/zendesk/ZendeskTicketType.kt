package com.woocommerce.android.support.zendesk

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class ZendeskTicketType(
    val form: Long,
    val categoryName: String,
    val subcategoryName: String,
    val mandatoryTags: List<String> = emptyList(),
    val forbiddenTags: List<String> = emptyList(),
    val additionalTags: List<String>
) : Parcelable {
    fun buildFullTagListWith(conditionalTags: List<String>) =
        (mandatoryTags + conditionalTags + additionalTags)
            .filter { forbiddenTags.contains(it).not() }

    @Parcelize
    data class MobileApp(
        private val extraTags: List<String>
    ) : ZendeskTicketType(
        form = TicketCustomField.wooMobileFormID,
        categoryName = ZendeskConstants.mobileAppCategory,
        subcategoryName = ZendeskConstants.mobileSubcategoryValue,
        mandatoryTags = listOf(ZendeskTags.mobileApp),
        additionalTags = extraTags
    )

    @Parcelize
    data class InPersonPayments(
        private val extraTags: List<String>
    ) : ZendeskTicketType(
        form = TicketCustomField.wooMobileFormID,
        categoryName = ZendeskConstants.mobileAppCategory,
        subcategoryName = ZendeskConstants.mobileSubcategoryValue,
        mandatoryTags = listOf(
            ZendeskTags.woocommerceMobileApps,
            ZendeskTags.productAreaAppsInPersonPayments
        ),
        additionalTags = extraTags
    )

    @Parcelize
    data class Payments(
        private val extraTags: List<String>
    ) : ZendeskTicketType(
        form = TicketCustomField.wooFormID,
        categoryName = ZendeskConstants.supportCategory,
        subcategoryName = ZendeskConstants.paymentsSubcategoryValue,
        mandatoryTags = listOf(
            ZendeskTags.paymentsProduct,
            ZendeskTags.paymentsProductArea,
            ZendeskTags.mobileAppWooTransfer,
            ZendeskTags.supportCategoryTag,
            ZendeskTags.paymentSubcategoryTag
        ),
        forbiddenTags = listOf(ZendeskTags.jetpackTag),
        additionalTags = extraTags
    )

    @Parcelize
    data class WooPlugin(
        private val extraTags: List<String>
    ) : ZendeskTicketType(
        form = TicketCustomField.wooFormID,
        categoryName = ZendeskConstants.supportCategory,
        subcategoryName = "",
        mandatoryTags = listOf(
            ZendeskTags.woocommerceCore,
            ZendeskTags.mobileAppWooTransfer,
            ZendeskTags.supportCategoryTag
        ),
        forbiddenTags = listOf(ZendeskTags.jetpackTag),
        additionalTags = extraTags
    )

    @Parcelize
    data class OtherPlugins(
        private val extraTags: List<String>
    ) : ZendeskTicketType(
        form = TicketCustomField.wooFormID,
        categoryName = ZendeskConstants.supportCategory,
        subcategoryName = ZendeskConstants.storeSubcategoryValue,
        mandatoryTags = listOf(
            ZendeskTags.productAreaWooExtensions,
            ZendeskTags.mobileAppWooTransfer,
            ZendeskTags.supportCategoryTag,
            ZendeskTags.storeSubcategoryTag
        ),
        forbiddenTags = listOf(ZendeskTags.jetpackTag),
        additionalTags = extraTags
    )
}
