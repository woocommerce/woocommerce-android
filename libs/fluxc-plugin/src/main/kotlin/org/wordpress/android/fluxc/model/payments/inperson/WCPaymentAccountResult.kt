package org.wordpress.android.fluxc.model.payments.inperson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.Deserializer
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus.StoreCurrencies
import java.lang.reflect.Type

data class WCPaymentAccountResult(
    @SerializedName("status")
    val status: WCPaymentAccountStatus,
    @SerializedName("has_pending_requirements")
    val hasPendingRequirements: Boolean,
    @SerializedName("has_overdue_requirements")
    val hasOverdueRequirements: Boolean,
    @SerializedName("current_deadline")
    val currentDeadline: Long?,
    /**
     * An alphanumeric string set by the merchant, e.g. `MYSTORE.COM`
     * See https://stripe.com/docs/statement-descriptors
     */
    @SerializedName("statement_descriptor")
    val statementDescriptor: String?,
    @SerializedName("store_currencies")
    val storeCurrencies: StoreCurrencies,

    /**
     * A two character country code, e.g. `US`
     * See https://stripe.com/docs/api/accounts/object#account_object-country
     */
    @SerializedName("country")
    val country: String,
    /**
     * A boolean flag indicating if this Account is test/live.
     */
    @SerializedName("is_live")
    val isLive: Boolean,

    /**
     * A boolean flag indicating if the test mode on the site is enabled. When "null" the state is unknown (the most
     * probable reason is that the site is using outdated version of WCPay or Stripe Extension plugin).
     */
    @SerializedName("test_mode")
    val testMode: Boolean?
) {
    /**
     * Represents all of the possible Site Plugin Statuses in enum form
     */
    @JsonAdapter(Deserializer::class)
    enum class WCPaymentAccountStatus {
        /**
         * This is the normal state for a fully functioning WCPay or Stripe Extension account. The merchant should be
         * able to collect card present payments.
         */
        COMPLETE,

        /**
         * This account has provided enough information to process payments and receive payouts.
         * More information will eventually be required when they process enough volume.
         */
        ENABLED,

        /**
         * This state occurs when there is required business information missing from the account.
         * If `hasOverdueRequirements` is also true, then the deadline for providing that information HAS PASSED and
         * the merchant will probably NOT be able to collect card present payments.
         * Otherwise, if `hasPendingRequirements` is true, then the deadline for providing that information has not yet
         * passed.
         * The deadline is available in `currentDeadline` and the merchant will probably be able to collect card present
         * payments until the deadline.
         * Otherwise, if neither `hasOverdueRequirements` nor `hasPendingRequirements` is true, then the account is
         * under review by Stripe and the merchant will probably not be able to collect card present payments.
         */
        RESTRICTED,

        /**
         * This state occurs when there is required business information missing from the account but the
         * currentDeadline hasn't passed yet (aka there are no overdueRequirements). The merchant will probably be able
         * to collect card present payments.
         */
        RESTRICTED_SOON,

        /**
         * This state occurs when our payment processor rejects the merchant account due to suspected fraudulent
         * activity. The merchant will NOT be able to collect card present payments.
         */
        REJECTED_FRAUD,

        /** This state occurs when our payment processor rejects the merchant account due to terms of
         * service violation(s). The merchant will NOT be able to collect card present payments.
         */
        REJECTED_TERMS_OF_SERVICE,

        /**
         * This state occurs when our payment processor rejects the merchant account due to sanctions/being
        on a watch list. The merchant will NOT be able to collect card present payments.
         */
        REJECTED_LISTED,

        /**
         * This state occurs when our payment processor rejects the merchant account due to any other reason.
         * The merchant will NOT be able to collect card present payments.
         */
        REJECTED_OTHER,

        /**
         * This state occurs when the merchant hasn't  on-boarded yet. The merchant will NOT be able to
         * collect card present payments.
         */
        NO_ACCOUNT,

        /**
         * This state occurs when the self-hosted site responded in a way we don't recognize.
         */
        UNKNOWN,

        /**
         * This state occurs when the account is early in an account's lifetime.
         */
        PENDING_VERIFICATION;

        class Deserializer : JsonDeserializer<WCPaymentAccountStatus> {
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type?,
                context: JsonDeserializationContext?
            ): WCPaymentAccountStatus =
                    when (json.asString) {
                        "complete" -> COMPLETE
                        "enabled" -> ENABLED
                        "restricted" -> RESTRICTED
                        "restricted_soon" -> RESTRICTED_SOON
                        "rejected.fraud" -> REJECTED_FRAUD
                        "rejected.terms_of_service" -> REJECTED_TERMS_OF_SERVICE
                        "rejected.listed" -> REJECTED_LISTED
                        "rejected.other" -> REJECTED_OTHER
                        "NOACCOUNT", "" -> NO_ACCOUNT
                        "pending_verification" -> PENDING_VERIFICATION
                        else -> UNKNOWN
                    }
        }

        data class StoreCurrencies(
            /**
             * A three character lowercase currency code, e.g. `usd`
             * See https://stripe.com/docs/api/accounts/object#account_object-default_currency
             */

            @SerializedName("default")
            val default: String,
            @SerializedName("supported")
            val supportedCurrencies: List<String>
        )
    }
}
