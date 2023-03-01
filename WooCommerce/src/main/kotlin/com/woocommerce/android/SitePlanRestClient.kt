package com.woocommerce.android

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import java.lang.reflect.Type
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

    suspend fun fetchSitePlans(site: SiteModel): WPComGsonRequestBuilder.Response<Map<Int, SitePlanDto>> {
        val url = "https://public-api.wordpress.com/rest/v1.3/sites/${site.siteId}/plans/"

        val type: Type = object : TypeToken<Map<Int, SitePlanDto>>() {}.type

        return wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            emptyMap(),
            clazz = JsonObject::class.java
        ).let { originalResponse ->
            when (originalResponse) {
                is WPComGsonRequestBuilder.Response.Success -> {
                    WPComGsonRequestBuilder.Response.Success(
                        data = gson.fromJson(originalResponse.data, type)
                    )
                }
                is WPComGsonRequestBuilder.Response.Error ->
                    WPComGsonRequestBuilder.Response.Error(error = originalResponse.error)
            }
        }
    }

    data class SitePlanDto(
        @SerializedName("expiry") val expirationDate: String?
    )
}
