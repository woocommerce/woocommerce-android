package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.MediaId

object MediaIdGenerator {
    fun generate(localSiteId: Int, filePath: String, timestamp: Long): MediaId {
        require(filePath.isNotEmpty()) { "filePath cannot be empty for media ID generation" }
        val combined = "$localSiteId:$filePath:$timestamp"
        return MediaId(combined.hashCode())
    }
}
