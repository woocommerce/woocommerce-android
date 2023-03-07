package com.woocommerce.android.apifaker.models

internal sealed interface EndpointType {
    object WPApi : EndpointType
    object WPCom : EndpointType

    data class Custom(val host: String) : EndpointType
}
