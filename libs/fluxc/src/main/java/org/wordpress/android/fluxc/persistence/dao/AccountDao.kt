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

    suspend fun getDefaultAccount(): AccountEntity? =
        getAccountById(DEFAULT_ACCOUNT_LOCAL_ID)

    @Query(
        """
        SELECT * FROM AccountEntity
        WHERE id = :id
        """
    )
    protected abstract suspend fun getAccountById(id: Int): AccountEntity?

    @Upsert
    abstract suspend fun upsert(account: AccountEntity)

    suspend fun updateDefaultUsername(username: String): Int =
        updateUsername(DEFAULT_ACCOUNT_LOCAL_ID, username)

    @Query(
        """
        UPDATE AccountEntity
        SET userName = :username
        WHERE id = :id
        """
    )
    protected abstract suspend fun updateUsername(id: Int, username: String): Int

    suspend fun deleteDefaultAccount(): Int =
        deleteAccount(DEFAULT_ACCOUNT_LOCAL_ID)

    @Query(
        """
        DELETE FROM AccountEntity
        WHERE id = :id
        """
    )
    protected abstract suspend fun deleteAccount(id: Int): Int
}
