package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.WCProductComponent

object ComponentMapper {
    fun toAppModel(databaseModel: WCProductComponent): Component{
        return Component(
            id = databaseModel.id,
            title = databaseModel.title,
            description = databaseModel.description,
            queryType = QueryType.fromValue(databaseModel.queryType),
            queryIds = databaseModel.queryIds,
            defaultOptionId = databaseModel.defaultOptionId.toLongOrNull(),
            thumbnailUrl = databaseModel.thumbnailUrl
        )
    }
}
