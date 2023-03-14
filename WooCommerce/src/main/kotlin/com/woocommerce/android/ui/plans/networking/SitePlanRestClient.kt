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
import java.time.LocalDate
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

    suspend fun addEcommercePlanTrial(siteRemoteId: Long): WPComGsonRequestBuilder.Response<Unit> {
        val url = WPCOMREST.sites.site(siteRemoteId)
            .ecommerce_trial
            .add
            .plan_slug("ecommerce-trial-bundle-monthly")
            .urlV1_1

        return wpComGsonRequestBuilder.syncPostRequest<Unit>(
            this,
            url,
            emptyMap(),
            body = emptyMap(),
            Unit::class.java
        )
    }

    private fun parseDateOrNull(rawDate: String?): LocalDate? = rawDate?.let {
        LocalDate.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    data class SitePlanDto(
        @SerializedName("user_facing_expiry") val expirationDate: String?
    )
}
