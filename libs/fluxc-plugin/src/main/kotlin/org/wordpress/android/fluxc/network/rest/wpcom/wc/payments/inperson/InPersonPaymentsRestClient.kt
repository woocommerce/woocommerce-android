package org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.inperson

import com.android.volley.VolleyError
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentError
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.CAPTURE_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.MISSING_ORDER
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.PAYMENT_ALREADY_CAPTURED
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentResponsePayload
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentChargeApiResult
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationError
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationErrorType
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationResult
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentTransactionsSummaryResult
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationResult.StoreAddress
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.STRIPE
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject
import org.wordpress.android.util.AppLog

class InPersonPaymentsRestClient @Inject constructor(
    private val wooNetwork: WooNetwork,
    private val gson: Gson,
) {
    suspend fun fetchConnectionToken(
        activePlugin: InPersonPaymentsPluginType,
        site: SiteModel
    ): WooPayload<ConnectionTokenApiResponse> {
        val url = when (activePlugin) {
            WOOCOMMERCE_PAYMENTS -> WOOCOMMERCE.payments.connection_tokens.pathV3
            STRIPE -> WOOCOMMERCE.wc_stripe.connection_tokens.pathV3
        }
        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = ConnectionTokenApiResponse::class.java
        )

        return response.toWooPayload()
    }

    suspend fun capturePayment(
        activePlugin: InPersonPaymentsPluginType,
        site: SiteModel,
        paymentId: String,
        orderId: Long
    ): WCCapturePaymentResponsePayload {
        val url = when (activePlugin) {
            WOOCOMMERCE_PAYMENTS -> WOOCOMMERCE.payments.orders.id(orderId).capture_terminal_payment.pathV3
            STRIPE -> WOOCOMMERCE.wc_stripe.orders.order(orderId).capture_terminal_payment.pathV3
        }
        val body = mapOf(
            "payment_intent_id" to paymentId
        )
        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = body,
            clazz = CapturePaymentApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let { data ->
                    WCCapturePaymentResponsePayload(site, paymentId, orderId, data.status)
                } ?: WCCapturePaymentResponsePayload(
                    mapToCapturePaymentError(
                        error = null,
                        message = "status field is null, but isError == false"
                    ),
                    site,
                    paymentId,
                    orderId
                )
            }
            is WPAPIResponse.Error -> {
                WCCapturePaymentResponsePayload(
                    mapToCapturePaymentError(
                        response.error,
                        response.error.message ?: "Unexpected error"
                    ),
                    site,
                    paymentId,
                    orderId
                )
            }
        }
    }

    suspend fun loadAccount(
        activePlugin: InPersonPaymentsPluginType,
        site: SiteModel
    ): WooPayload<WCPaymentAccountResult> {
        val url = when (activePlugin) {
            WOOCOMMERCE_PAYMENTS -> WOOCOMMERCE.payments.accounts.pathV3
            STRIPE -> WOOCOMMERCE.wc_stripe.account.summary.pathV3
        }
        val params = mapOf("_fields" to ACCOUNT_REQUESTED_FIELDS)

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = WCPaymentAccountResult::class.java
        )

        return response.toWooPayload()
    }

    suspend fun getStoreLocationForSite(
        activePlugin: InPersonPaymentsPluginType,
        site: SiteModel
    ): WCTerminalStoreLocationResult {
        val url = when (activePlugin) {
            WOOCOMMERCE_PAYMENTS -> WOOCOMMERCE.payments.terminal.locations.store.pathV3
            STRIPE -> WOOCOMMERCE.wc_stripe.terminal.locations.store.pathV3
        }

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = StoreLocationApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let { data ->
                    WCTerminalStoreLocationResult(
                        locationId = data.id,
                        displayName = data.displayName,
                        liveMode = data.liveMode,
                        address = StoreAddress(
                            city = data.address?.city,
                            country = data.address?.country,
                            line1 = data.address?.line1,
                            line2 = data.address?.line2,
                            postalCode = data.address?.postalCode,
                            state = data.address?.state
                        )
                    )
                } ?: WCTerminalStoreLocationResult(
                    mapToStoreLocationForSiteError(
                        error = null,
                        message = "status field is null, but isError == false"
                    )
                )
            }
            is WPAPIResponse.Error -> {
                WCTerminalStoreLocationResult(
                    mapToStoreLocationForSiteError(
                        response.error,
                        response.error.message ?: "Unexpected error"
                    )
                )
            }
        }
    }

    suspend fun fetchPaymentCharge(
        activePlugin: InPersonPaymentsPluginType,
        chargeId: String,
        site: SiteModel
    ): WooPayload<WCPaymentChargeApiResult> {
        val url = when (activePlugin) {
            WOOCOMMERCE_PAYMENTS -> WOOCOMMERCE.payments.charges.charge(chargeId).pathV3
            STRIPE -> WOOCOMMERCE.wc_stripe.charges.charge(chargeId).pathV3
        }

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = WCPaymentChargeApiResult::class.java
        )

        return response.toWooPayload()
    }

    suspend fun fetchTransactionsSummary(
        activePlugin: InPersonPaymentsPluginType,
        site: SiteModel,
        dateAfter: String? = null,
    ): WooPayload<WCPaymentTransactionsSummaryResult> {
        val url = when (activePlugin) {
            WOOCOMMERCE_PAYMENTS -> WOOCOMMERCE.payments.transactions.summary.pathV3
            STRIPE -> error("Stripe does not support fetching transactions summary")
        }
        val params = mutableMapOf<String, String>()
        dateAfter?.let { params["date_after"] = it }
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = WCPaymentTransactionsSummaryResult::class.java
        )

        return response.toWooPayload()
    }

    private fun mapToCapturePaymentError(
        error: WPAPINetworkError?,
        message: String
    ): WCCapturePaymentError {
        val type = when {
            error == null -> GENERIC_ERROR
            error.errorCode == "wcpay_missing_order" -> MISSING_ORDER
            error.errorCode == "wcpay_payment_uncapturable" -> PAYMENT_ALREADY_CAPTURED
            error.errorCode == "wcpay_capture_error" -> CAPTURE_ERROR
            error.errorCode == "wcpay_server_error" -> SERVER_ERROR
            error.type == GenericErrorType.TIMEOUT -> NETWORK_ERROR
            error.type == GenericErrorType.NO_CONNECTION -> NETWORK_ERROR
            error.type == GenericErrorType.NETWORK_ERROR -> NETWORK_ERROR
            else -> GENERIC_ERROR
        }
        return WCCapturePaymentError(
            type,
            message,
            extraData = error?.volleyError?.getExtraData()
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun VolleyError.getExtraData(): Map<String, Any>? {
        val jsonString = this.networkResponse?.data?.toString(Charsets.UTF_8)
        return try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            return gson.fromJson<Map<String, Any>>(jsonString, mapType)["data"] as Map<String, Any>?
        } catch (e: Throwable) {
            AppLog.e(AppLog.T.API, "Error parsing volley error $jsonString", e)
            null
        }
    }

    private fun mapToStoreLocationForSiteError(error: WPAPINetworkError?, message: String):
        WCTerminalStoreLocationError {
        val type = when {
            error == null -> WCTerminalStoreLocationErrorType.GenericError
            error.errorCode == "store_address_is_incomplete" -> {
                if (error.message.isNullOrBlank()) WCTerminalStoreLocationErrorType.GenericError
                else WCTerminalStoreLocationErrorType.MissingAddress(error.message)
            }
            error.errorCode == "postal_code_invalid" -> WCTerminalStoreLocationErrorType.InvalidPostalCode
            error.type == GenericErrorType.TIMEOUT -> WCTerminalStoreLocationErrorType.NetworkError
            error.type == GenericErrorType.NO_CONNECTION -> WCTerminalStoreLocationErrorType.NetworkError
            error.type == GenericErrorType.NETWORK_ERROR -> WCTerminalStoreLocationErrorType.NetworkError
            else -> WCTerminalStoreLocationErrorType.GenericError
        }
        return WCTerminalStoreLocationError(type, message)
    }

    companion object {
        private const val ACCOUNT_REQUESTED_FIELDS: String =
            "status,has_pending_requirements,has_overdue_requirements,current_deadline,statement_descriptor," +
                "store_currencies,country,is_live,test_mode"
    }
}
