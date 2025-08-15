package org.wordpress.android.fluxc.network.rest

import com.android.volley.NetworkResponse

data class GsonResponseWrapper<T>(
    val data: T,
    val networkResponse: NetworkResponse
)
