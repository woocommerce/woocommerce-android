package com.woocommerce.android.model

import com.woocommerce.android.extensions.fastStripHtml
import org.wordpress.android.fluxc.model.WCProductComponent

object ComponentMapper {
    fun toAppModel(databaseModel: WCProductComponent): Component{
        return Component(
            id = databaseModel.id,
            title = databaseModel.title.fastStripHtml(),
            description = databaseModel.description.fastStripHtml(),
            queryType = QueryType.fromValue(databaseModel.queryType),
            queryIds = databaseModel.queryIds,
            defaultOptionId = databaseModel.defaultOptionId.toLongOrNull(),
            thumbnailUrl = databaseModel.thumbnailUrl
        )
    }
}
