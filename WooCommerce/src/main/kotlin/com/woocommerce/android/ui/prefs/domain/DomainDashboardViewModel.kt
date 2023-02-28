package com.woocommerce.android.ui.prefs.domain

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.DOMAIN_CHANGE_CURRENT_DOMAIN
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.UserRole
import com.woocommerce.android.support.help.HelpOrigin.DOMAIN_CHANGE
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.prefs.domain.DomainDashboardViewModel.ViewState.DashboardState
import com.woocommerce.android.ui.prefs.domain.DomainDashboardViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.prefs.domain.DomainDashboardViewModel.ViewState.ErrorState.ErrorType.ACCESS_ERROR
import com.woocommerce.android.ui.prefs.domain.DomainDashboardViewModel.ViewState.ErrorState.ErrorType.GENERIC_ERROR
import com.woocommerce.android.ui.prefs.domain.DomainDashboardViewModel.ViewState.LoadingState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class DomainDashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val repository: DomainChangeRepository,
    private val userEligibilityFetcher: UserEligibilityFetcher
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val LEARN_MORE_URL = "https://wordpress.com/go/tutorials/what-is-a-domain-name/"
        private const val NO_DOMAIN = "<NO DOMAIN>"
    }

    private var hasFreeCredits = false

    private val _viewState = savedStateHandle.getStateFlow<ViewState>(this, LoadingState)
    val viewState = _viewState.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DOMAIN_CHANGE_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to DOMAIN_CHANGE_CURRENT_DOMAIN
            )
        )

        launch {
            if (hasAccess()) {
                loadData()
            } else {
                _viewState.update { ErrorState(ACCESS_ERROR) }
            }
        }
    }

    private suspend fun hasAccess(): Boolean {
        return async {
            val userInfo = userEligibilityFetcher.fetchUserInfo()
            return@async (userInfo.isSuccess && userInfo.getOrNull()?.roles?.contains(UserRole.Administrator) == true)
        }.await()
    }

    private suspend fun loadData() {
        val domainsAsync = async { repository.fetchSiteDomains() }
        val planAsync = async { repository.fetchActiveSitePlan() }
        val domainsResult = domainsAsync.await()
        val planResult = planAsync.await()

        hasFreeCredits = planResult.getOrNull()?.hasDomainCredit == true

        if (domainsResult.isFailure) {
            _viewState.update { ErrorState() }
        } else {
            val freeDomain = domainsResult.getOrNull()?.firstOrNull { it.wpcomDomain }
            val paidDomains = domainsResult.getOrNull()
                ?.filter { !it.wpcomDomain && it.domain != null } ?: emptyList()
            if (freeDomain != null) {
                _viewState.update {
                    DashboardState(
                        wpComDomain = DashboardState.Domain(
                            url = freeDomain.domain ?: NO_DOMAIN,
                            isPrimary = freeDomain.primaryDomain
                        ),
                        paidDomains = paidDomains.map { domain ->
                            DashboardState.Domain(
                                url = domain.domain!!,
                                renewalDate = domain.expiry,
                                isPrimary = domain.primaryDomain
                            )
                        },
                        isDomainClaimBannerVisible = hasFreeCredits
                    )
                }
            }
        }
    }

    fun onCancelPressed() {
        analyticsTrackerWrapper.track(AnalyticsEvent.DOMAIN_CHANGE_DISMISSED)
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onHelpPressed() {
        triggerEvent(MultiLiveEvent.Event.NavigateToHelpScreen(DOMAIN_CHANGE))
    }

    fun onFindDomainButtonTapped() {
        analyticsTrackerWrapper.track(AnalyticsEvent.DOMAIN_CHANGE_SEARCH_FOR_DOMAIN_BUTTON_TAPPED)
        triggerEvent(NavigateToDomainSearch(hasFreeCredits))
    }

    fun onLearnMoreButtonTapped() {
        triggerEvent(ShowMoreAboutDomains(LEARN_MORE_URL))
    }

    sealed interface ViewState : Parcelable {
        @Parcelize
        object LoadingState : ViewState

        @Parcelize
        data class ErrorState(val errorType: ErrorType = GENERIC_ERROR) : ViewState {
            enum class ErrorType {
                GENERIC_ERROR,
                ACCESS_ERROR
            }
        }

        @Parcelize
        data class DashboardState(
            val wpComDomain: Domain,
            val isDomainClaimBannerVisible: Boolean,
            val paidDomains: List<Domain>
        ) : ViewState {
            @Parcelize
            data class Domain(
                val url: String,
                val renewalDate: String? = null,
                val isPrimary: Boolean
            ) : Parcelable
        }
    }

    data class NavigateToDomainSearch(val hasFreeCredits: Boolean) : MultiLiveEvent.Event()
    data class ShowMoreAboutDomains(val url: String) : MultiLiveEvent.Event()
}
