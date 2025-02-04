package com.woocommerce.android.apifaker.ui

internal sealed class Screen(val baseRoute: String) {
    object Home : Screen("home") {
        fun route() = baseRoute
    }

    object EndpointDetails : Screen("/endpoint-details") {
        const val endpointIdArgumentName = "endpointId"
        val routeTemplate = "$baseRoute?$endpointIdArgumentName={$endpointIdArgumentName}"

        fun route(endpointId: Long) = "$baseRoute?$endpointIdArgumentName=$endpointId"

        fun routeForCreation() = baseRoute
    }
}
