package org.wordpress.android.fluxc.store.pos.localcatalog

sealed class PosLocalCatalogError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    data class NetworkError(
        val errorMessage: String,
        val code: String? = null
    ) : PosLocalCatalogError(errorMessage)

    data class DatabaseError(
        val errorMessage: String,
        val throwable: Throwable? = null
    ) : PosLocalCatalogError(errorMessage, throwable)

    object EmptyResponse : PosLocalCatalogError("Empty response from server")

    data class InvalidResponse(val errorMessage: String) : PosLocalCatalogError(errorMessage)

    data class UnknownError(
        val throwable: Throwable
    ) : PosLocalCatalogError(throwable.message ?: "Unknown error", throwable)
}
