package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.dto

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto

data class AddOnGroupDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("restrict_to_category_ids") val categoryIds: List<Long>?,
    @SerializedName("fields") val addons: List<RemoteAddonDto>
)
