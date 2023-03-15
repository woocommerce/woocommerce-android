package com.woocommerce.android.ui.plans.networking

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.ui.plans.domain.SitePlan
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import java.lang.reflect.Type
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Named

class SitePlanRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val gson: Gson
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {

    suspend fun fetchSitePlans(site: SiteModel): WPComGsonRequestBuilder.Response<Map<Int, SitePlan>> {
        val url = WPCOMREST.sites.site(site.siteId).plans.urlV1_3
        val type: Type = object : TypeToken<Map<Int, SitePlanDto>>() {}.type

        return wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            emptyMap(),
            JsonObject::class.java
        ).let { originalResponse ->
            when (originalResponse) {
                is WPComGsonRequestBuilder.Response.Success -> {
                    WPComGsonRequestBuilder.Response.Success(
                        gson.fromJson<Map<Int, SitePlanDto>?>(originalResponse.data, type)
                            .mapValues { (_, sitePlanDto) ->
                                SitePlan(
                                    expirationDate = parseDateOrNull(sitePlanDto.expirationDate)
                                )
                            }
                    )
                }
                is WPComGsonRequestBuilder.Response.Error ->
                    WPComGsonRequestBuilder.Response.Error(originalResponse.error)
            }
        }
    }

    private fun parseDateOrNull(rawDate: String?) = rawDate?.let {
        ZonedDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    data class SitePlanDto(
        @SerializedName("user_facing_expiry") val expirationDate: String?
    )
}
