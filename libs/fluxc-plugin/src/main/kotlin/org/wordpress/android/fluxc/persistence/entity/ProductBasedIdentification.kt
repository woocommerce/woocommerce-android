package org.wordpress.android.fluxc.persistence.entity

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

data class ProductBasedIdentification(
    val productRemoteId: RemoteId,
    val localSiteId: LocalId
)
