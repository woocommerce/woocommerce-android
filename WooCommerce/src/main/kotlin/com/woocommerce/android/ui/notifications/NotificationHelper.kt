package com.woocommerce.android.ui.notifications

import org.wordpress.android.fluxc.model.NotificationModel
import org.wordpress.android.fluxc.tools.FormattableRangeType.POST

object NotificationHelper {
    fun getProductName(notif: NotificationModel): String? {
        return notif.subject?.get(0)?.let { block ->
            val fullText = block.text ?: ""
            val post = block.ranges?.asSequence()?.filter { it.rangeType() == POST }?.firstOrNull()
            post?.let {
                val start = it.indices?.get(0) ?: 0
                val end = it.indices?.get(1) ?: 0

                fullText.substring(start, end)
            }
        }
    }

    fun getProductUrl(notif: NotificationModel): String? {
        return notif.subject?.get(0)?.let { block ->
            block.ranges?.asSequence()?.filter { it.rangeType() == POST }?.firstOrNull()?.url
        }
    }
}
