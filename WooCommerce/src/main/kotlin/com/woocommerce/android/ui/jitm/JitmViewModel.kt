package com.woocommerce.android.ui.jitm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.MyStoreUtmProvider
import com.woocommerce.android.ui.mystore.MyStoreViewModel
import com.woocommerce.android.ui.payments.banner.BannerState
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.store.JitmStore
import javax.inject.Inject

@HiltViewModel
class JitmViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val jitmStore: JitmStore,
    private val jitmTracker: JitmTracker,
    private val myStoreUtmProvider: MyStoreUtmProvider,
    private val queryParamsEncoder: QueryParamsEncoder,
    private val selectedSite: SelectedSite,
) : ScopedViewModel(savedState) {
    private val _jitmState: MutableLiveData<BannerState> = MutableLiveData()
    val jitmState: LiveData<BannerState> = _jitmState

    val jitmCtaClickedEvent: MutableLiveData<CtaClick> = MultiLiveEvent()

    init {
        fetchJitms()
    }

    fun fetchJitms() {
        fetchJitms(savedState.get<String>(JITM_MESSAGE_PATH_KEY)!!)
    }

    private fun fetchJitms(jitmMessagePath: String) {
        launch {
            val response = jitmStore.fetchJitmMessage(
                selectedSite.get(),
                jitmMessagePath,
                queryParamsEncoder.getEncodedQueryParams(),
            )
            populateResultToUI(response, jitmMessagePath)
        }
    }

    private fun populateResultToUI(response: WooResult<Array<JITMApiResponse>>, jitmMessagePath: String) {
        if (response.isError) {
            jitmTracker.trackJitmFetchFailure(MyStoreViewModel.UTM_SOURCE, response.error.type, response.error.message)
            WooLog.e(WooLog.T.JITM, "Failed to fetch JITM for the message path $jitmMessagePath")
            return
        }

        jitmTracker.trackJitmFetchSuccess(
            MyStoreViewModel.UTM_SOURCE,
            response.model?.getOrNull(0)?.id,
            response.model?.size
        )
        response.model?.getOrNull(0)?.let { model: JITMApiResponse ->
            jitmTracker.trackJitmDisplayed(
                MyStoreViewModel.UTM_SOURCE,
                model.id,
                model.featureClass
            )
            _jitmState.value = BannerState.DisplayBannerState(
                onPrimaryActionClicked = {
                    onJitmCtaClicked(
                        id = model.id,
                        featureClass = model.featureClass,
                        url = model.cta.link
                    )
                },
                onDismissClicked = {
                    onJitmDismissClicked(
                        model.id,
                        model.featureClass
                    )
                },
                title = UiString.UiStringText(model.content.message),
                description = UiString.UiStringText(model.content.description),
                primaryActionLabel = UiString.UiStringText(model.cta.message),
                chipLabel = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_new)
            )
        } ?: run {
            _jitmState.value = BannerState.HideBannerState
            WooLog.i(WooLog.T.JITM, "No JITM Campaign in progress")
        }
    }

    private fun onJitmCtaClicked(
        id: String,
        featureClass: String,
        url: String
    ) {
        jitmTracker.trackJitmCtaTapped(
            MyStoreViewModel.UTM_SOURCE,
            id,
            featureClass
        )
        jitmCtaClickedEvent.value =
            CtaClick(
                myStoreUtmProvider.getUrlWithUtmParams(
                    source = MyStoreViewModel.UTM_SOURCE,
                    id = id,
                    featureClass = featureClass,
                    siteId = selectedSite.getIfExists()?.siteId,
                    url = url
                )
            )
    }

    private fun onJitmDismissClicked(jitmId: String, featureClass: String) {
        _jitmState.value = BannerState.HideBannerState
        jitmTracker.trackJitmDismissTapped(MyStoreViewModel.UTM_SOURCE, jitmId, featureClass)
        launch {
            jitmStore.dismissJitmMessage(selectedSite.get(), jitmId, featureClass).also { response ->
                when {
                    response.model != null && response.model!! -> {
                        jitmTracker.trackJitmDismissSuccess(
                            MyStoreViewModel.UTM_SOURCE,
                            jitmId,
                            featureClass
                        )
                    }
                    else -> jitmTracker.trackJitmDismissFailure(
                        MyStoreViewModel.UTM_SOURCE,
                        jitmId,
                        featureClass,
                        response.error?.type,
                        response.error?.message
                    )
                }
            }
        }
    }

    data class CtaClick(val url: String) : MultiLiveEvent.Event()

    companion object {
        const val JITM_MESSAGE_PATH_KEY = "jitm_message_path_key"
    }
}


