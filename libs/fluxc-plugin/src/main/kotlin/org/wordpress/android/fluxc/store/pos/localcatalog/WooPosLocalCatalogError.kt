package org.wordpress.android.fluxc.store.pos.localcatalog

sealed class WooPosLocalCatalogError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    data class NetworkError(
        val errorMessage: String,
        val code: String? = null
    ) : WooPosLocalCatalogError(errorMessage)

    data class DatabaseError(
        val errorMessage: String,
        val throwable: Throwable? = null
    ) : WooPosLocalCatalogError(errorMessage, throwable)

    object EmptyResponse : WooPosLocalCatalogError("Empty response from server")

    data class InvalidResponse(val errorMessage: String) : WooPosLocalCatalogError(errorMessage)

    data class UnknownError(
        val throwable: Throwable
    ) : WooPosLocalCatalogError(throwable.message ?: "Unknown error", throwable)
}
