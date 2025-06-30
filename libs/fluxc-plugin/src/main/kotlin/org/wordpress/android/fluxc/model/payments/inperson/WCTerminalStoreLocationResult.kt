package org.wordpress.android.fluxc.model.payments.inperson

import org.wordpress.android.fluxc.FluxCError
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationErrorType.GenericError

data class WCTerminalStoreLocationResult(
    val locationId: String?,
    val displayName: String?,
    val liveMode: Boolean?,
    val address: StoreAddress?
) : Payload<WCTerminalStoreLocationError?>() {
    constructor(
        error: WCTerminalStoreLocationError
    ) : this(null, null, null, null) {
        this.error = error
    }

    data class StoreAddress(
        val city: String?,
        val country: String?,
        val line1: String?,
        val line2: String?,
        val postalCode: String?,
        val state: String?
    )
}

data class WCTerminalStoreLocationError(
    val type: WCTerminalStoreLocationErrorType = GenericError,
    val message: String = ""
) : FluxCError

sealed class WCTerminalStoreLocationErrorType {
    object GenericError : WCTerminalStoreLocationErrorType()
    data class MissingAddress(val addressEditingUrl: String) : WCTerminalStoreLocationErrorType()
    object InvalidPostalCode : WCTerminalStoreLocationErrorType()
    object ServerError : WCTerminalStoreLocationErrorType()
    object NetworkError : WCTerminalStoreLocationErrorType()
}
