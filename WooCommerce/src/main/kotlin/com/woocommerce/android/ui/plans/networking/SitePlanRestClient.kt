package com.woocommerce.android.ui.plans.networking

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.ui.plans.domain.FREE_TRIAL_PLAN_ID
import com.woocommerce.android.ui.plans.domain.SitePlan
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
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

    suspend fun fetchCurrentPlanDetails(site: SiteModel): Response<SitePlan?> {
        val url = WPCOMREST.sites.site(site.siteId).plans.urlV1_3
        val type: Type = object : TypeToken<Map<Int, SitePlanDto>>() {}.type

        return wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            emptyMap(),
            JsonObject::class.java
        ).let { originalResponse ->
            when (originalResponse) {
                is Success -> {
                    Success(
                        gson.fromJson<Map<Int, SitePlanDto>?>(originalResponse.data, type)
                            .filterValues { it.currentPlan == true }
                            .toList()
                            .firstOrNull()
                            ?.let { (id, sitePlanDto) ->
                                if (sitePlanDto.expirationDate == null) {
                                    null
                                } else {
                                    SitePlan(
                                        name = sitePlanDto.name,
                                        expirationDate = parseDateOrNull(sitePlanDto.expirationDate),
                                        type = determineSitePlanType(id)
                                    )
                                }
                            }
                    )
                }

                is Error ->
                    Error(originalResponse.error)
            }
        }
    }

    suspend fun fetchSitePlans(site: SiteModel): Response<Map<Int, SitePlan?>> {
        val url = WPCOMREST.sites.site(site.siteId).plans.urlV1_3
        val type: Type = object : TypeToken<Map<Int, SitePlanDto>>() {}.type

        return wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            emptyMap(),
            JsonObject::class.java
        ).let { originalResponse ->
            when (originalResponse) {
                is Success -> {
                    Success(
                        gson.fromJson<Map<Int, SitePlanDto>?>(originalResponse.data, type)
                            .mapValues { (id, sitePlanDto) ->
                                if (sitePlanDto.expirationDate == null) {
                                    null
                                } else {
                                    SitePlan(
                                        name = sitePlanDto.name,
                                        expirationDate = parseDateOrNull(sitePlanDto.expirationDate),
                                        type = determineSitePlanType(id)
                                    )
                                }
                            }
                    )
                }
                is Error -> Error(originalResponse.error)
            }
        }
    }

    private fun determineSitePlanType(id: Int) =
        if (id.toLong() == FREE_TRIAL_PLAN_ID) SitePlan.Type.FREE_TRIAL else SitePlan.Type.OTHER

    private fun parseDateOrNull(rawDate: String): ZonedDateTime = rawDate.let {
        ZonedDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    suspend fun addEcommercePlanTrial(siteRemoteId: Long): Response<Unit> {
        val url = WPCOMREST.sites.site(siteRemoteId)
            .ecommerce_trial
            .add
            .plan_slug("ecommerce-trial-bundle-monthly")
            .urlV1_1

        return wpComGsonRequestBuilder.syncPostRequest(
            this,
            url,
            params = emptyMap(),
            body = emptyMap(),
            Unit::class.java
        )
    }

    data class SitePlanDto(
        @SerializedName("product_name") val name: String,
        @SerializedName("expiry") val expirationDate: String?,
        @SerializedName("current_plan") val currentPlan: Boolean?,
    )
}
