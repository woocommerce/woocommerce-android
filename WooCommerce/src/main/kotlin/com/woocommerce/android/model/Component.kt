package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Component(
    val id: Long,
    val title: String,
    val description: String,
    val queryType: QueryType,
    val queryIds: List<Long>,
    val defaultOption: Long,
    val thumbnailUrl: String?
):Parcelable

enum class QueryType(val value: String){
    PRODUCT("product_ids"),
    CATEGORY("category_ids");
    companion object {
        private val valueMap = QueryType.values().associateBy(QueryType::value)
        fun fromValue(value: String) = valueMap[value]
    }
}
