package org.wordpress.android.fluxc.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MediaEntity")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val localSiteId: Int,
    val localPostId: Int,
    val mediaId: Long,
    val postId: Long,
    val uploadDate: String?,
    val url: String,
    val thumbnailUrl: String?,
    val fileName: String?,
    val filePath: String?,
    val mimeType: String?,
    val title: String?,
    val caption: String,
    val description: String,
    val alt: String,
    val uploadState: String?,
    val markedLocallyAsFeatured: Boolean
)
