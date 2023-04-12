package com.woocommerce.android.model

data class ComponentProduct(
    val id: Long,
    val title: String,
    val description: String,
    val queryType: QueryType,
    val queryIds: List<Long>,
    val defaultOption: Long,
    val thumbnailUrl: String?
)

enum class QueryType(val value: String){
    PRODUCT("product_ids"),
    CATEGORY("category_ids");
    companion object {
        private val valueMap = QueryType.values().associateBy(QueryType::value)
        fun fromValue(value: String) = valueMap[value]
    }
}
