package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "CouponEmails",
    foreignKeys = [
        ForeignKey(
            entity = CouponEntity::class,
            parentColumns = ["id", "localSiteId"],
            childColumns = ["couponId", "localSiteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    primaryKeys = ["couponId", "localSiteId", "email"]
)
data class CouponEmailEntity(
    val couponId: RemoteId,
    val localSiteId: LocalId,
    val email: String
)
