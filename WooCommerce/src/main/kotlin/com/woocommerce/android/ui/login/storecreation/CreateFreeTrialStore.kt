package com.woocommerce.android.ui.login.storecreation

import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_ADDRESS_ALREADY_EXISTS
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow

class CreateFreeTrialStore @Inject constructor(
    private val repository: StoreCreationRepository
) {
    private val error = MutableStateFlow<StoreCreationErrorType?>(null)

    suspend fun createFreeTrialSite(
        storeDomain: String,
        storeName: String
    ) = repository.createNewFreeTrialSite(
        StoreCreationRepository.SiteCreationData(
            siteDesign = PlansViewModel.NEW_SITE_THEME,
            domain = storeDomain,
            title = storeName,
            segmentId = null
        ),
        PlansViewModel.NEW_SITE_LANGUAGE_ID,
        TimeZone.getDefault().id
    ).recoverIfSiteExists(storeDomain)

    private suspend fun StoreCreationResult<Long>.recoverIfSiteExists(
        storeDomain: String
    ) = run { this as? StoreCreationResult.Failure<Long> }
        ?.takeIf { it.type == SITE_ADDRESS_ALREADY_EXISTS }
        ?.let { repository.getSiteByUrl(storeDomain) }
        ?.let { StoreCreationResult.Success(it.siteId) }
        ?: this

    private suspend fun <T : Any?> StoreCreationResult<T>.ifSuccessfulThen(
        successAction: suspend (T) -> Unit
    ) {
        when (this) {
            is StoreCreationResult.Success -> successAction(this.data)
            is StoreCreationResult.Failure -> {
                error.emit(this.type)
            }
        }
    }
}
