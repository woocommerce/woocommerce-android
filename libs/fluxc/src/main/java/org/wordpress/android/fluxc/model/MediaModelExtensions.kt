package org.wordpress.android.fluxc.model

fun MediaModel.withUploadState(uploadState: String?) = copy(uploadState = uploadState)

fun MediaModel.withId(id: Int) = copy(id = id)

fun MediaModel.withMediaId(mediaId: Long) = copy(mediaId = mediaId)

fun MediaModel.withLocalIds(
    id: Int,
    markedLocallyAsFeatured: Boolean
) = copy(
    id = id,
    markedLocallyAsFeatured = markedLocallyAsFeatured
)
