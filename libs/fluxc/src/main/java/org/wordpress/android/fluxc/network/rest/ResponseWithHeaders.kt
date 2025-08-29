package org.wordpress.android.fluxc.network.rest

data class ResponseWithHeaders<D>(val data: D?, val headers: List<Header>)
