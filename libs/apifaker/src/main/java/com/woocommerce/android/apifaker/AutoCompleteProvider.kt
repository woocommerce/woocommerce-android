package com.woocommerce.android.apifaker

import com.woocommerce.android.apifaker.models.ApiType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.annotations.endpoint.JPAPIEndpoint
import org.wordpress.android.fluxc.annotations.endpoint.WCWPAPIEndpoint
import org.wordpress.android.fluxc.annotations.endpoint.WPAPIEndpoint
import org.wordpress.android.fluxc.annotations.endpoint.WPComEndpoint
import org.wordpress.android.fluxc.annotations.endpoint.WPComV2Endpoint
import org.wordpress.android.fluxc.generated.endpoint.JPAPI
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import java.lang.reflect.Modifier
import javax.inject.Inject

internal class AutoCompleteProvider @Inject constructor() {
    private val wpApiEndpointsList by lazy {
        getEndpointsFromClass(WPAPI::class.java, WPAPIEndpoint::class.java) { "/$urlV2" }
            .map { AutoCompleteSuggestion(it) } +
            getEndpointsFromClass(WOOCOMMERCE::class.java, WCWPAPIEndpoint::class.java) { findBestUrl() }
                .map { AutoCompleteSuggestion(it, isNameSpaceConfirmed = false) } +
            getEndpointsFromClass(JPAPI::class.java, JPAPIEndpoint::class.java) { pathV4 }
                .map { AutoCompleteSuggestion(it) }
    }

    private val wpComEndpointsList by lazy {
        fun String.excludingWPComPrefix() = replace(WPCOM_PREFIX, "")

        getEndpointsFromClass(WPCOMREST::class.java, WPComEndpoint::class.java) { urlV1_1.excludingWPComPrefix() }
            .map { AutoCompleteSuggestion(it, isNameSpaceConfirmed = false) } +
            getEndpointsFromClass(WPCOMV2::class.java, WPComV2Endpoint::class.java) { url.excludingWPComPrefix() }
                .map { AutoCompleteSuggestion(it) }
    }

    suspend fun provideAutoCompleteSuggestions(
        endpointType: ApiType,
        query: String
    ): List<AutoCompleteSuggestion> = withContext(Dispatchers.Default) {
        when (endpointType) {
            ApiType.WPApi -> wpApiEndpointsList
            ApiType.WPCom -> wpComEndpointsList
            else -> emptyList()
        }
            .filter { it.endpoint.contains(query) && it.endpoint != query }
            .take(SUGGESTIONS_LIMIT)
    }

    private inline fun <reified T : Any> getEndpointsFromClass(
        clazz: Class<*>,
        fieldClazz: Class<T>,
        noinline urlGetter: T.() -> String
    ): List<String> {
        val endpoints = clazz.declaredFields
            .filter {
                Modifier.isStatic(it.modifiers) && fieldClazz.isAssignableFrom(it.type)
            }
            .map { Pair(it.get(null), it.type) }
            .flatMap { (field, type) -> (field as T).getNestedEndpoints(T::class.java, type) + field }

        return endpoints.map { it.urlGetter() }
            .map { it.replace(STRING_PLACEHOLDER, "%").replace("$INT_PLACEHOLDER", "%") }
    }

    @Suppress("UNCHECKED_CAST", "SpreadOperator")
    private fun <T : Any> T.getNestedEndpoints(
        endpointTypeClass: Class<T>,
        currentClass: Class<*>
    ): List<T> {
        val fieldEndpoints = currentClass.declaredFields
            .filter { endpointTypeClass.isAssignableFrom(it.type) }
            .map { it.get(this) as T }

        val methodEndpoints = currentClass.declaredMethods
            .filter { endpointTypeClass.isAssignableFrom(it.returnType) }
            .map {
                val args = it.parameters.map { param ->
                    when (param.type) {
                        String::class.java -> STRING_PLACEHOLDER
                        Int::class.java, Long::class.java -> INT_PLACEHOLDER
                        else -> error("Unsupported type: ${param.type}")
                    }
                } as List<Any>

                it.invoke(this, *args.toTypedArray()) as T
            }

        return fieldEndpoints + methodEndpoints +
            fieldEndpoints.flatMap { it.getNestedEndpoints(endpointTypeClass, it::class.java) } +
            methodEndpoints.flatMap { it.getNestedEndpoints(endpointTypeClass, it::class.java) }
    }

    /**
     * Try to find the best namespace for the given endpoint.
     *
     * This is not perfect, but it should offer better suggestions than just using pathV3 by default.
     */
    private fun WCWPAPIEndpoint.findBestUrl(): String = when {
        endpoint.startsWith(WOOCOMMERCE.leaderboards.endpoint) ||
            endpoint.startsWith(WOOCOMMERCE.admin.endpoint) ||
            endpoint.startsWith(WOOCOMMERCE.reports.endpoint) -> pathV4Analytics

        endpoint.startsWith(WOOCOMMERCE.product_add_ons.endpoint) -> pathV1Addons

        endpoint.startsWith(WOOCOMMERCE.options.endpoint) ||
            endpoint.startsWith(WOOCOMMERCE.onboarding.endpoint) -> pathWcAdmin

        endpoint.startsWith(WOOCOMMERCE.tracker.endpoint) -> pathWcTelemetry

        endpoint.startsWith(WOOCOMMERCE.connect.endpoint) -> pathV1

        else -> pathV3
    }

    companion object {
        private const val SUGGESTIONS_LIMIT = 10
        private const val STRING_PLACEHOLDER = "string"
        private const val INT_PLACEHOLDER = 999
        private const val WPCOM_PREFIX = "https://public-api.wordpress.com"
    }
}

data class AutoCompleteSuggestion(
    val endpoint: String,
    val isNameSpaceConfirmed: Boolean = true
)
