package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

interface MediaIdGenerator {
    fun generate(filePath: String): LocalId
}
