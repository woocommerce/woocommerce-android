package com.woocommerce.android.ui.login.storecreation

import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Loading
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Success
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_ADDRESS_ALREADY_EXISTS
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.Companion.NEW_SITE_LANGUAGE_ID
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

class CreateFreeTrialStore @Inject constructor(
    private val repository: StoreCreationRepository
) {
    private val _state = MutableStateFlow<StoreCreationState>(StoreCreationState.Idle)
    val state: StateFlow<StoreCreationState> = _state

    suspend operator fun invoke(
        storeDomain: String?,
        storeName: String?
    ) = flow {
        _state.value = Loading

        val creationData = StoreCreationRepository.SiteCreationData(
            siteDesign = PlansViewModel.NEW_SITE_THEME,
            domain = storeDomain,
            title = storeName,
            segmentId = null
        )

        val result = repository.createNewFreeTrialSite(creationData, NEW_SITE_LANGUAGE_ID, TimeZone.getDefault().id)
            .recoverIfSiteExists(storeDomain)
            .also { _state.value = it.asCreationState() }


        if (result is StoreCreationResult.Success<Long>) {
            emit(result.data)
        }
    }

    private suspend fun StoreCreationResult<Long>.recoverIfSiteExists(
        storeDomain: String?
    ) = run { this as? StoreCreationResult.Failure<Long> }
        ?.takeIf { it.type == SITE_ADDRESS_ALREADY_EXISTS }
        ?.let { repository.getSiteByUrl(storeDomain) }
        ?.let { StoreCreationResult.Success(it.siteId) }
        ?: this

    private fun StoreCreationResult<Long>.asCreationState() = when (this) {
        is StoreCreationResult.Success -> Success
        is StoreCreationResult.Failure -> StoreCreationState.Error(this.type)
    }

    sealed class StoreCreationState {
        object Idle : StoreCreationState()
        object Loading : StoreCreationState()
        object Success : StoreCreationState()
        data class Error(val type: StoreCreationErrorType) : StoreCreationState()
    }
}
