package org.wordpress.android.fluxc.persistence

import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.model.AccountModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountStorePersistence @Inject constructor() {
    fun getDefaultAccount(): AccountModel? = runBlocking {
        AccountSqlUtils.getDefaultAccount()
    }

    fun insertOrUpdateDefaultAccount(account: AccountModel) = runBlocking {
        AccountSqlUtils.insertOrUpdateDefaultAccount(account)
    }

    fun updateUsername(account: AccountModel, username: String) = runBlocking {
        AccountSqlUtils.updateUsername(account, username)
    }

    fun deleteAccount(account: AccountModel) = runBlocking {
        AccountSqlUtils.deleteAccount(account)
    }
}
