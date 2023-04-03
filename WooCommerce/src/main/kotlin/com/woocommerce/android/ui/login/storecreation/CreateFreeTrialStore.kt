package com.woocommerce.android.ui.login.storecreation

import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Loading
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_ADDRESS_ALREADY_EXISTS
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow

class CreateFreeTrialStore @Inject constructor(
    private val repository: StoreCreationRepository
) {
    val state = MutableStateFlow<StoreCreationState>(StoreCreationState.Idle)

    suspend fun createFreeTrialSite(
        storeDomain: String,
        storeName: String
    ) {
        state.value = Loading

        val result = repository.createNewFreeTrialSite(
            StoreCreationRepository.SiteCreationData(
                siteDesign = PlansViewModel.NEW_SITE_THEME,
                domain = storeDomain,
                title = storeName,
                segmentId = null
            ),
            PlansViewModel.NEW_SITE_LANGUAGE_ID,
            TimeZone.getDefault().id
        )

        state.value = result
            .recoverIfSiteExists(storeDomain)
            .asCreationState()
    }

    private suspend fun StoreCreationResult<Long>.recoverIfSiteExists(
        storeDomain: String
    ) = run { this as? StoreCreationResult.Failure<Long> }
        ?.takeIf { it.type == SITE_ADDRESS_ALREADY_EXISTS }
        ?.let { repository.getSiteByUrl(storeDomain) }
        ?.let { StoreCreationResult.Success(it.siteId) }
        ?: this

    private fun StoreCreationResult<Long>.asCreationState() = when (this) {
        is StoreCreationResult.Success -> StoreCreationState.Success(this.data)
        is StoreCreationResult.Failure -> StoreCreationState.Error(this.type)
    }

    sealed class StoreCreationState {
        object Idle : StoreCreationState()
        object Loading : StoreCreationState()
        data class Success(val siteId: Long) : StoreCreationState()
        data class Error(val type: StoreCreationErrorType) : StoreCreationState()
    }
}
