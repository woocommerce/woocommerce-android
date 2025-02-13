package org.wordpress.android.fluxc.model.payments.inperson

import com.google.gson.annotations.SerializedName

data class WCPaymentChargeApiResult(
    @SerializedName("amount")
    val amount: Int?,
    @SerializedName("amount_captured")
    val amountCaptured: Int?,
    @SerializedName("amount_refunded")
    val amountRefunded: Int?,
    @SerializedName("application")
    val application: String?,
    @SerializedName("application_fee")
    val applicationFee: String?,
    @SerializedName("application_fee_amount")
    val applicationFeeAmount: Int?,
    @SerializedName("authorization_code")
    val authorizationCode: String?,
    @SerializedName("balance_transaction")
    val balanceTransaction: BalanceTransaction?,
    @SerializedName("billing_details")
    val billingDetails: BillingDetails?,
    @SerializedName("calculated_statement_descriptor")
    val calculatedStatementDescriptor: String?,
    @SerializedName("captured")
    val captured: Boolean?,
    @SerializedName("created")
    val created: Int?,
    @SerializedName("currency")
    val currency: String?,
    @SerializedName("customer")
    val customer: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("destination")
    val destination: Any?,
    @SerializedName("dispute")
    val dispute: Any?,
    @SerializedName("disputed")
    val disputed: Boolean?,
    @SerializedName("failure_code")
    val failureCode: Any?,
    @SerializedName("failure_message")
    val failureMessage: Any?,
    @SerializedName("fraud_details")
    val fraudDetails: List<Any?>?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("invoice")
    val invoice: Any?,
    @SerializedName("level3")
    val level3: Level3?,
    @SerializedName("livemode")
    val livemode: Boolean?,
    @SerializedName("metadata")
    val metadata: Metadata?,
    @SerializedName("object")
    val objectX: String?,
    @SerializedName("on_behalf_of")
    val onBehalfOf: Any?,
    @SerializedName("order")
    val order: Order?,
    @SerializedName("outcome")
    val outcome: Outcome?,
    @SerializedName("paid")
    val paid: Boolean?,
    @SerializedName("payment_intent")
    val paymentIntent: String?,
    @SerializedName("payment_method")
    val paymentMethod: String?,
    @SerializedName("payment_method_details")
    val paymentMethodDetails: PaymentMethodDetails?,
    @SerializedName("receipt_email")
    val receiptEmail: Any?,
    @SerializedName("receipt_number")
    val receiptNumber: Any?,
    @SerializedName("receipt_url")
    val receiptUrl: String?,
    @SerializedName("refunded")
    val refunded: Boolean?,
    @SerializedName("refunds")
    val refunds: Refunds?,
    @SerializedName("review")
    val review: Any?,
    @SerializedName("shipping")
    val shipping: Any?,
    @SerializedName("source")
    val source: Any?,
    @SerializedName("source_transfer")
    val sourceTransfer: Any?,
    @SerializedName("statement_descriptor")
    val statementDescriptor: String?,
    @SerializedName("statement_descriptor_suffix")
    val statementDescriptorSuffix: Any?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("transfer_data")
    val transferData: Any?,
    @SerializedName("transfer_group")
    val transferGroup: Any?
) {
    data class BalanceTransaction(
        @SerializedName("amount")
        val amount: Int?,
        @SerializedName("available_on")
        val availableOn: Int?,
        @SerializedName("created")
        val created: Int?,
        @SerializedName("currency")
        val currency: String?,
        @SerializedName("description")
        val description: String?,
        @SerializedName("exchange_rate")
        val exchangeRate: Any?,
        @SerializedName("fee")
        val fee: Int?,
        @SerializedName("fee_details")
        val feeDetails: List<FeeDetail?>?,
        @SerializedName("id")
        val id: String?,
        @SerializedName("net")
        val net: Int?,
        @SerializedName("object")
        val objectX: String?,
        @SerializedName("reporting_category")
        val reportingCategory: String?,
        @SerializedName("source")
        val source: String?,
        @SerializedName("status")
        val status: String?,
        @SerializedName("type")
        val type: String?
    ) {
        data class FeeDetail(
            @SerializedName("amount")
            val amount: Int?,
            @SerializedName("application")
            val application: String?,
            @SerializedName("currency")
            val currency: String?,
            @SerializedName("description")
            val description: String?,
            @SerializedName("type")
            val type: String?
        )
    }

    data class BillingDetails(
        @SerializedName("address")
        val address: Address?,
        @SerializedName("email")
        val email: Any?,
        @SerializedName("formatted_address")
        val formattedAddress: String?,
        @SerializedName("name")
        val name: Any?,
        @SerializedName("phone")
        val phone: Any?
    ) {
        data class Address(
            @SerializedName("city")
            val city: Any?,
            @SerializedName("country")
            val country: Any?,
            @SerializedName("line1")
            val line1: Any?,
            @SerializedName("line2")
            val line2: Any?,
            @SerializedName("postal_code")
            val postalCode: Any?,
            @SerializedName("state")
            val state: Any?
        )
    }

    data class Level3(
        @SerializedName("customer_reference")
        val customerReference: String?,
        @SerializedName("line_items")
        val lineItems: List<LineItem?>?,
        @SerializedName("merchant_reference")
        val merchantReference: String?,
        @SerializedName("shipping_amount")
        val shippingAmount: Int?,
        @SerializedName("shipping_from_zip")
        val shippingFromZip: String?
    ) {
        data class LineItem(
            @SerializedName("discount_amount")
            val discountAmount: Int?,
            @SerializedName("product_code")
            val productCode: String?,
            @SerializedName("product_description")
            val productDescription: String?,
            @SerializedName("quantity")
            val quantity: Int?,
            @SerializedName("tax_amount")
            val taxAmount: Int?,
            @SerializedName("unit_cost")
            val unitCost: Int?
        )
    }

    data class Metadata(
        @SerializedName("order_id")
        val orderId: String?,
        @SerializedName("payment_type")
        val paymentType: String?,
        @SerializedName("paymentintent.storename")
        val paymentintentStorename: String?,
        @SerializedName("reader_ID")
        val readerID: String?,
        @SerializedName("reader_model")
        val readerModel: String?,
        @SerializedName("site_url")
        val siteUrl: String?
    )

    data class Order(
        @SerializedName("customer_url")
        val customerUrl: String?,
        @SerializedName("number")
        val number: String?,
        @SerializedName("subscriptions")
        val subscriptions: List<Any?>?,
        @SerializedName("url")
        val url: String?
    )

    data class Outcome(
        @SerializedName("network_status")
        val networkStatus: String?,
        @SerializedName("reason")
        val reason: Any?,
        @SerializedName("risk_level")
        val riskLevel: String?,
        @SerializedName("seller_message")
        val sellerMessage: String?,
        @SerializedName("type")
        val type: String?
    )

    data class PaymentMethodDetails(
        @SerializedName("card_present")
        val cardDetails: CardDetails?,
        @SerializedName("interac_present")
        val interacCardDetails: CardDetails?,
        @SerializedName("type")
        val type: String?
    ) {
        data class CardDetails(
            @SerializedName("amount_authorized")
            val amountAuthorized: Int?,
            @SerializedName("brand")
            val brand: String?,
            @SerializedName("cardholder_name")
            val cardholderName: String?,
            @SerializedName("country")
            val country: String?,
            @SerializedName("emv_auth_data")
            val emvAuthData: String?,
            @SerializedName("exp_month")
            val expMonth: Int?,
            @SerializedName("exp_year")
            val expYear: Int?,
            @SerializedName("fingerprint")
            val fingerprint: String?,
            @SerializedName("funding")
            val funding: String?,
            @SerializedName("generated_card")
            val generatedCard: String?,
            @SerializedName("last4")
            val last4: String?,
            @SerializedName("network")
            val network: String?,
            @SerializedName("overcapture_supported")
            val overcaptureSupported: Boolean?,
            @SerializedName("read_method")
            val readMethod: String?,
            @SerializedName("receipt")
            val receipt: Receipt?
        ) {
            data class Receipt(
                @SerializedName("account_type")
                val accountType: String?,
                @SerializedName("application_cryptogram")
                val applicationCryptogram: String?,
                @SerializedName("application_preferred_name")
                val applicationPreferredName: String?,
                @SerializedName("authorization_code")
                val authorizationCode: Any?,
                @SerializedName("authorization_response_code")
                val authorizationResponseCode: String?,
                @SerializedName("cardholder_verification_method")
                val cardholderVerificationMethod: String?,
                @SerializedName("dedicated_file_name")
                val dedicatedFileName: String?,
                @SerializedName("terminal_verification_results")
                val terminalVerificationResults: String?,
                @SerializedName("transaction_status_information")
                val transactionStatusInformation: String?
            )
        }
    }

    @Suppress("ConstructorParameterNaming")
    data class Refunds(
        @SerializedName("data")
        val `data`: List<Any?>?,
        @SerializedName("has_more")
        val hasMore: Boolean?,
        @SerializedName("object")
        val objectX: String?,
        @SerializedName("total_count")
        val totalCount: Int?,
        @SerializedName("url")
        val url: String?
    )
}
