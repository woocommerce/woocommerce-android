package com.woocommerce.android.apifaker.models

internal sealed interface EndpointType {
    companion object {
        fun defaultValues(): List<EndpointType> = listOf(WPApi, WPCom, Custom(""))
    }

    object WPApi : EndpointType
    object WPCom : EndpointType

    data class Custom(val host: String) : EndpointType
}
