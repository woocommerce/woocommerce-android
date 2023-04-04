package com.woocommerce.android.ui.login.storecreation

import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Failed
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Finished
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Loading
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_ADDRESS_ALREADY_EXISTS
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository.SiteCreationData
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.Companion.NEW_SITE_LANGUAGE_ID
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.Companion.NEW_SITE_THEME
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.util.TimeZone
import javax.inject.Inject

@ViewModelScoped
class CreateFreeTrialStore @Inject constructor(
    private val repository: StoreCreationRepository
) {
    private val _state = MutableStateFlow<StoreCreationState>(StoreCreationState.Idle)
    val state: StateFlow<StoreCreationState> = _state

    /**
     * Triggers the creation of a new free trial site given a domain and a name.
     * If the site already exists, it will try to retrieve the site ID from the API.
     *
     *  @return a [flow] that will emit the Store ID if the creation is successful, null otherwise
     *
     *  To observe the creation progress, use [state]
     */
    suspend operator fun invoke(
        storeDomain: String?,
        storeName: String?
    ) = flow {
        _state.value = Loading

        val result = repository.createNewFreeTrialSite(
            SiteCreationData(null, NEW_SITE_THEME, storeDomain, storeName),
            NEW_SITE_LANGUAGE_ID,
            TimeZone.getDefault().id
        ).recoverIfSiteExists(storeDomain)

        when (result) {
            is StoreCreationResult.Success -> {
                _state.value = Finished
                emit(result.data)
            }
            is StoreCreationResult.Failure -> {
                _state.value = Failed(result.type)
                emit(null)
            }
        }
    }

    private suspend fun StoreCreationResult<Long>.recoverIfSiteExists(
        storeDomain: String?
    ) = run { this as? StoreCreationResult.Failure<Long> }
        ?.takeIf { it.type == SITE_ADDRESS_ALREADY_EXISTS }
        ?.let { repository.getSiteByUrl(storeDomain) }
        ?.let { StoreCreationResult.Success(it.siteId) }
        ?: this

    sealed class StoreCreationState {
        object Idle : StoreCreationState()
        object Loading : StoreCreationState()
        object Finished : StoreCreationState()
        data class Failed(val type: StoreCreationErrorType) : StoreCreationState()
    }
}
