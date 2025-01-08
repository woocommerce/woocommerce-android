package com.woocommerce.android.apifaker.models

internal sealed interface ApiType {
    companion object {
        fun defaultValues(): List<ApiType> = listOf(WPApi, WPCom, Custom(""))
    }

    data object WPApi : ApiType
    data object WPCom : ApiType

    data class Custom(val host: String) : ApiType
}
