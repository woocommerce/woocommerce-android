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

    fun getDefaultAccount(): AccountModel? = runBlocking {
        accountDao.getDefaultAccount()?.let { accountMapper.toModel(it) }
    }

    fun insertOrUpdateDefaultAccount(account: AccountModel) = runBlocking {
        accountDao.upsert(accountMapper.toEntity(account))
    }

    fun updateUsername(username: String) = runBlocking {
        accountDao.updateDefaultUsername(username)
    }

    fun deleteAccount() = runBlocking {
        accountDao.deleteDefaultAccount()
    }
}
