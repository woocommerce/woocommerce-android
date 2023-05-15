package com.woocommerce.android.ui.prefs

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.util.dispatchAndAwait
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class PrivacySettingsRepository @Inject constructor(
    private val accountStore: AccountStore,
    private val dispatcher: Dispatcher,
) {
    companion object {
        private const val SETTING_TRACKS_OPT_OUT = "tracks_opt_out"
    }

    suspend fun updateTracksSetting(enable: Boolean): Result<Unit> {
        val action =
            AccountActionBuilder.newPushSettingsAction(
                AccountStore.PushAccountSettingsPayload().apply {
                    params = mapOf(SETTING_TRACKS_OPT_OUT to !enable)
                }
            )

        val event: AccountStore.OnAccountChanged =
            dispatcher.dispatchAndAwait<AccountStore.PushAccountSettingsPayload?, AccountStore.OnAccountChanged>(
                action
            )

        return when {
            event.isError -> Result.failure(OnChangedException(event.error))
            else -> Result.success(Unit)
        }
    }

    suspend fun fetchAccountSettings(): Result<Unit> {
        val event: AccountStore.OnAccountChanged =
            dispatcher.dispatchAndAwait(AccountActionBuilder.newFetchSettingsAction())

        return when {
            event.isError -> Result.failure(OnChangedException(event.error))
            else -> Result.success(Unit)
        }
    }

    fun userOptOutFromTracks(): Boolean {
        return accountStore.account.tracksOptOut
    }

    fun isUserWPCOM(): Boolean {
        return accountStore.hasAccessToken()
    }
}
