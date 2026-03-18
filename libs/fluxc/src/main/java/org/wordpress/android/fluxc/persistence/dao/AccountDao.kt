package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.persistence.entity.AccountEntity

@Dao
internal abstract class AccountDao {
    companion object {
        const val DEFAULT_ACCOUNT_LOCAL_ID = 1
    }

    @Query(
        """
        SELECT * FROM AccountEntity
        WHERE id = $DEFAULT_ACCOUNT_LOCAL_ID
        """
    )
    abstract suspend fun getDefaultAccount(): AccountEntity?

    @Upsert
    abstract suspend fun upsert(account: AccountEntity)

    @Query(
        """
        UPDATE AccountEntity
        SET userName = :username
        WHERE id = $DEFAULT_ACCOUNT_LOCAL_ID
        """
    )
    abstract suspend fun updateDefaultUsername(username: String): Int

    @Query(
        """
        DELETE FROM AccountEntity
        WHERE id = $DEFAULT_ACCOUNT_LOCAL_ID
        """
    )
    abstract suspend fun deleteDefaultAccount(): Int
}
