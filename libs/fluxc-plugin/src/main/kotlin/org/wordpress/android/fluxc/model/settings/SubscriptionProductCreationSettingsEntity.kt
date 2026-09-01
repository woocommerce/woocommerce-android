package org.wordpress.android.fluxc.model.settings

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(tableName = "SubscriptionProductCreationSettings")
data class SubscriptionProductCreationSettingsEntity(
    @PrimaryKey val localSiteId: LocalId,
    val isSimpleSubscriptionCreationEnabled: Boolean?,
    val isVariableSubscriptionCreationEnabled: Boolean?,
)
