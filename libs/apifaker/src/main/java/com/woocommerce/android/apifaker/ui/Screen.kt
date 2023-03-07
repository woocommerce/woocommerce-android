package com.woocommerce.android.apifaker.ui

internal sealed class Screen(val baseRoute: String) {
    object Home : Screen("home") {
        fun route() = baseRoute
    }

    object EndpointDetails : Screen("/endpoint-details") {
        const val endpointIdArgumentName = "endpointId"
        fun route(endpointId: Int) = "$baseRoute/endpointId=$endpointId"

        fun routeForCreation() = baseRoute
    }
}
