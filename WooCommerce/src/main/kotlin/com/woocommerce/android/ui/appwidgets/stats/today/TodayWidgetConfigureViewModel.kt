package com.woocommerce.android.ui.widgets.stats.today

import android.os.Parcelable
import android.text.TextUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R.string
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.widgets.WidgetColorMode
import com.woocommerce.android.ui.widgets.stats.today.TodayWidgetConfigureViewModel.TodayWidgetNavigationTarget.ViewWidgetColorSelectionList
import com.woocommerce.android.ui.widgets.stats.today.TodayWidgetConfigureViewModel.TodayWidgetNavigationTarget.ViewWidgetSiteSelectionList
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class TodayWidgetConfigureViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
    private val wooCommerceStore: WooCommerceStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val resourceProvider: ResourceProvider,
) : ScopedViewModel(savedState), LifecycleOwner {
    private val lifecycleRegistry: LifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    private var appWidgetId: Int = -1

    private val mutableSites = MutableLiveData<List<SiteUiModel>>()
    val sites: LiveData<List<SiteUiModel>> = mutableSites

    val todayWidgetConfigureViewStateData = LiveDataDelegate(savedState, TodayWidgetConfigureViewState())
    private var todayWidgetConfigureViewState by todayWidgetConfigureViewStateData

    fun start(appWidgetId: Int) {
        this.appWidgetId = appWidgetId
        val siteId = selectedSite.getSelectedSiteId().toLong()
        if (siteId > -1) {
            siteStore.getSiteBySiteId(siteId)?.let { updateSelectedSite(toSiteUiModel(it)) }
        }

        val colorMode = appPrefsWrapper.getAppWidgetColor(appWidgetId)
        colorMode?.let { selectColor(it) }
    }

    fun addWidget() {
        val selectedSite = todayWidgetConfigureViewState.selectedSiteUiModel
        val selectedColorMode = todayWidgetConfigureViewState.selectedWidgetColorCode
        if (appWidgetId != -1 && selectedSite != null) {
            appPrefsWrapper.setAppWidgetSiteId(selectedSite.siteId, appWidgetId)
            appPrefsWrapper.setAppWidgetColor(selectedColorMode, appWidgetId)
            triggerEvent(WidgetAdded(appWidgetId))
        }
    }

    fun loadSites() {
        val sites = wooCommerceStore.getWooCommerceSites().map { toSiteUiModel(it) }
        mutableSites.postValue(sites)
    }

    private fun toSiteUiModel(site: SiteModel): SiteUiModel {
        val siteName = if (!TextUtils.isEmpty(site.name)) site.name
        else resourceProvider.getString(string.untitled)
        val domain = StringUtils.getSiteDomainAndPath(site)
        return SiteUiModel(site.siteId, site.iconUrl, siteName, domain)
    }

    fun selectSite(site: SiteUiModel) {
        triggerEvent(Exit)
        updateSelectedSite(site)
    }

    private fun updateSelectedSite(site: SiteUiModel) {
        todayWidgetConfigureViewState = todayWidgetConfigureViewState.copy(selectedSiteUiModel = site)
    }

    fun selectColor(color: WidgetColorMode) {
        todayWidgetConfigureViewState = todayWidgetConfigureViewState.copy(selectedWidgetColorCode = color)
    }

    fun getSelectedColor() = todayWidgetConfigureViewState.selectedWidgetColorCode

    fun onSiteSpinnerSelected() {
        val isLoggedIn = accountStore.hasAccessToken()
        if (isLoggedIn) {
            triggerEvent(ViewWidgetSiteSelectionList)
        } else triggerEvent(ShowSnackbar(string.stats_widget_log_in_message))
    }

    fun onColorSpinnerSelected() {
        val isLoggedIn = accountStore.hasAccessToken()
        if (isLoggedIn) {
            triggerEvent(ViewWidgetColorSelectionList)
        } else triggerEvent(ShowSnackbar(string.stats_widget_log_in_message))
    }

    sealed class TodayWidgetNavigationTarget : Event() {
        object ViewWidgetSiteSelectionList : TodayWidgetNavigationTarget()
        object ViewWidgetColorSelectionList : TodayWidgetNavigationTarget()
    }
    data class WidgetAdded(val appWidgetId: Int) : Event()

    @Parcelize
    data class SiteUiModel(
        val siteId: Long,
        val iconUrl: String?,
        val title: String?,
        val url: String?
    ) : Parcelable

    @Parcelize
    data class TodayWidgetConfigureViewState(
        val selectedSiteUiModel: SiteUiModel? = null,
        val selectedWidgetColorCode: WidgetColorMode = WidgetColorMode.LIGHT
    ) : Parcelable {
        val buttonEnabled: Boolean
            get() = selectedSiteUiModel?.title != null
    }
}
