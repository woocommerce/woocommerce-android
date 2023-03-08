package com.woocommerce.android.apifaker.models

internal sealed interface ApiType {
    companion object {
        fun defaultValues(): List<ApiType> = listOf(WPApi, WPCom, Custom(""))
    }

    object WPApi : ApiType
    object WPCom : ApiType

    data class Custom(val host: String) : ApiType
}
