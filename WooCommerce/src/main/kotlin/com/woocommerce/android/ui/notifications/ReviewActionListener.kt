package com.woocommerce.android.ui.notifications

import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus

interface ReviewActionListener {
    fun moderateComment(remoteNoteId: Long, comment: CommentModel, newStatus: CommentStatus)
}
