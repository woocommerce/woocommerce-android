package org.wordpress.android.fluxc.persistence

import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.model.AccountModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountStorePersistence @Inject constructor(
    private val database: WPAndroidDatabase,
    private val accountMapper: AccountMapper,
) {
    private val accountDao get() = database.accountDao()

    suspend fun getDefaultAccount(): AccountModel? =
        accountDao.getDefaultAccount()?.let { accountMapper.toModel(it) }

    suspend fun insertOrUpdateDefaultAccount(account: AccountModel) =
        accountDao.upsert(accountMapper.toEntity(account))

    suspend fun updateUsername(username: String) = accountDao.updateDefaultUsername(username)

    suspend fun deleteAccount() = accountDao.deleteDefaultAccount()

    // The blocking functions below are only for AccountStore, which is written in Java and can't call suspend functions

    fun getDefaultAccountBlocking(): AccountModel? = runBlocking { getDefaultAccount() }

    fun insertOrUpdateDefaultAccountBlocking(account: AccountModel) =
        runBlocking { insertOrUpdateDefaultAccount(account) }

    fun updateUsernameBlocking(username: String) = runBlocking { updateUsername(username) }

    fun deleteAccountBlocking() = runBlocking { deleteAccount() }
}
