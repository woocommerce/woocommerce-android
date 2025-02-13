package org.wordpress.android.fluxc.model.taxes

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "TaxRate",
    primaryKeys = ["id", "localSiteId"],
)
data class TaxRateEntity (
    val id: RemoteId,
    val localSiteId: LocalId,
    val country: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val rate: String? = null,
    val name: String? = null,
    val taxClass: String? = null,
)
