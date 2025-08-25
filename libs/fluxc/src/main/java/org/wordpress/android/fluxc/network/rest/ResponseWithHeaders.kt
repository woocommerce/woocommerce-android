package org.wordpress.android.fluxc.network.rest

import com.android.volley.Header

data class ResponseWithHeaders<D>(val data: D?, val headers: List<Header>)
