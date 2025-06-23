package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.user.WCUserModel

@Dao
abstract class UserDao {
    @Query(
        """
        SELECT * FROM UserEntity
        WHERE localSiteId = :siteId
        AND email = :userEmail
        """
    )
    abstract suspend fun getUserBySiteAndEmail(siteId: LocalId, userEmail: String): WCUserModel?

    @Upsert
    abstract suspend fun upsertUser(user: WCUserModel): Long
}
