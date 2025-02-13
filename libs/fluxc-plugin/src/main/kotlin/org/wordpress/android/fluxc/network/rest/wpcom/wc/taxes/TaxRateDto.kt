package org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.taxes.TaxRateEntity

data class TaxRateDto (
    val id: Long,
    val country: String?,
    val state: String?,
    @SerializedName("postcode") val postCode: String?,
    val city: String?,
    @SerializedName("postcodes") val postCodes: List<String>?,
    val cities: List<String>?,
    val rate: String?,
    val name: String?,
    val priority: Int?,
    val compound: Boolean?,
    val shipping: Boolean?,
    val order: Int?,
    @SerializedName("class") val taxClass: String?,
) {
    fun toDataModel(localSiteId: LocalId): TaxRateEntity =
        TaxRateEntity(
            id = RemoteId(id),
            localSiteId = localSiteId,
            country = country,
            state = state,
            postcode = postCode,
            city = city,
            rate = rate,
            name = name,
            taxClass = taxClass,
        )
}


