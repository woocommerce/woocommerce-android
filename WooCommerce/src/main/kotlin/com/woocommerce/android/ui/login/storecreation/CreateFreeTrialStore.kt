package com.woocommerce.android.ui.login.storecreation

import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Failed
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Finished
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Loading
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_ADDRESS_ALREADY_EXISTS
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository.SiteCreationData
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Failure
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Success
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.Companion.NEW_SITE_LANGUAGE_ID
import kotlinx.coroutines.flow.flow
import java.util.TimeZone
import javax.inject.Inject

class CreateFreeTrialStore @Inject constructor(
    private val repository: StoreCreationRepository
) {
    /**
     * Triggers the creation of a new free trial site given a domain and a name.
     * If the site already exists, it will try to retrieve the site ID from the API.
     *
     *  @return a [flow] that will emit the store creation state steps:
     *
     *  [Loading] -> [Finished] or [Failed]
     */
    suspend operator fun invoke(
        storeDomain: String?,
        storeName: String?,
        profilerData: NewStore.ProfilerData?,
        countryCode: String?
    ) = flow {
        emit(Loading)

        val result = repository.createNewFreeTrialSite(
            SiteCreationData(
                segmentId = null,
                domain = storeDomain,
                title = storeName,
                profilerData = profilerData,
                countryCode = countryCode
            ),
            NEW_SITE_LANGUAGE_ID,
            TimeZone.getDefault().id
        ).recoverIfSiteExists(storeDomain)

        when (result) {
            is Success -> emit(Finished(result.data))
            is Failure -> emit(Failed(result.type))
        }
    }

    private suspend fun StoreCreationResult<Long>.recoverIfSiteExists(
        storeDomain: String?
    ) = run { this as? Failure<Long> }
        ?.takeIf { it.type == SITE_ADDRESS_ALREADY_EXISTS }
        ?.let { repository.getSiteByUrl(storeDomain) }
        ?.let { Success(it.siteId) }
        ?: this

    sealed class StoreCreationState {
        object Loading : StoreCreationState()
        data class Finished(val siteId: Long) : StoreCreationState()
        data class Failed(val type: StoreCreationErrorType) : StoreCreationState()
    }
}
