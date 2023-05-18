package com.woocommerce.android.tools

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.observeEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentAccount @Inject constructor(
    private val accountStore: AccountStore,
    private val dispatcher: Dispatcher,
    @AppCoroutineScope coroutineScope: CoroutineScope,
) {
    private val state: MutableStateFlow<AccountModel> = MutableStateFlow(accountStore.account)

    fun observe(): Flow<AccountModel?> = state

    fun set(accountModel: AccountModel) {
        state.value = accountModel
    }

    init {
        coroutineScope.launch {
            dispatcher.observeEvents<OnAccountChanged>().collect {
                set(accountStore.account)
            }
        }
    }
}
