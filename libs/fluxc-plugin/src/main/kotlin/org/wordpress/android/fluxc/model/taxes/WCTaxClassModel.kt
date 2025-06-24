package org.wordpress.android.fluxc.model.taxes

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(
    tableName = "TaxClassEntity",
    primaryKeys = ["localSiteId", "slug"],
)
data class WCTaxClassModel(
    val localSiteId: LocalId,
    val name: String,
    val slug: String // The unique identifier for the tax class on the server
)
