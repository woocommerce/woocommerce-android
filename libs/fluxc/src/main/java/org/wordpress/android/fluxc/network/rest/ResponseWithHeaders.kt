package org.wordpress.android.fluxc.network.rest

data class ResponseWithHeaders<D> @JvmOverloads constructor(
    val data: D?,
    val headers: List<Header>,
    val statusCode: Int? = null
)
