package com.woocommerce.android.ui.jitm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.MyStoreUtmProvider
import com.woocommerce.android.ui.mystore.MyStoreViewModel
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.store.JitmStore
import javax.inject.Inject

private typealias Assets = Map<String, String>?

@HiltViewModel
class JitmViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val jitmStore: JitmStore,
    private val jitmTracker: JitmTracker,
    private val myStoreUtmProvider: MyStoreUtmProvider,
    private val queryParamsEncoder: QueryParamsEncoder,
    private val selectedSite: SelectedSite,
) : ScopedViewModel(savedState) {
    private val _jitmState: MutableLiveData<JitmState> = MutableLiveData()
    val jitmState: LiveData<JitmState> = _jitmState

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

            _jitmState.value = when ("modal") {
                JITM_TEMPLATE_MODAL -> JitmState.Modal(
                    onPrimaryActionClicked = { onJitmCtaClicked(model) },
                    onDismissClicked = { onJitmDismissClicked(model) },
                    title = UiString.UiStringText(model.content.message),
                    description = UiString.UiStringText("Did you know that you can take payments using your phone?"),
                    primaryActionLabel = UiString.UiStringText("Try Tap To Pay"),
                    backgroundImageUrl = model.assets?.get(JITM_ASSETS_BACKGROUND_IMAGE_KEY),
                )
                else -> JitmState.Banner(
                    onPrimaryActionClicked = { onJitmCtaClicked(model) },
                    onDismissClicked = { onJitmDismissClicked(model) },
                    title = UiString.UiStringText(model.content.message),
                    description = UiString.UiStringText(model.content.description),
                    primaryActionLabel = UiString.UiStringText(model.cta.message),
                    backgroundImage = model.assets.getBackgroundImage(),
                    badgeIcon = model.assets.getBadgeIcon(),
                )
            }
        } ?: run {
            _jitmState.value = JitmState.Hidden
            WooLog.i(WooLog.T.JITM, "No JITM Campaign in progress")
        }
    }

    private fun onJitmCtaClicked(model: JITMApiResponse) {
        jitmTracker.trackJitmCtaTapped(
            MyStoreViewModel.UTM_SOURCE,
            model.id,
            model.featureClass
        )
        triggerEvent(
            CtaClick(
                myStoreUtmProvider.getUrlWithUtmParams(
                    source = MyStoreViewModel.UTM_SOURCE,
                    id = model.id,
                    featureClass = model.featureClass,
                    siteId = selectedSite.getIfExists()?.siteId,
                    url = "https://woocommerce.com/mobile/payments/tap-to-pay"
                )
            )
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onJitmDismissClicked(model: JITMApiResponse) {
        _jitmState.value = JitmState.Hidden
//        jitmTracker.trackJitmDismissTapped(MyStoreViewModel.UTM_SOURCE, model.id, model.featureClass)
//        launch {
//            jitmStore.dismissJitmMessage(selectedSite.get(), model.id, model.featureClass).also { response ->
//                when {
//                    response.model != null && response.model!! -> {
//                        jitmTracker.trackJitmDismissSuccess(
//                            MyStoreViewModel.UTM_SOURCE,
//                            model.id,
//                            model.featureClass
//                        )
//                    }
//                    else -> jitmTracker.trackJitmDismissFailure(
//                        MyStoreViewModel.UTM_SOURCE,
//                        model.id,
//                        model.featureClass,
//                        response.error?.type,
//                        response.error?.message
//                    )
//                }
//            }
//        }
    }

    private fun Assets.getBackgroundImage() =
        this?.get(JITM_ASSETS_BACKGROUND_IMAGE_KEY)?.let { JitmState.Banner.LocalOrRemoteImage.Remote(it) }
            ?: JitmState.Banner.LocalOrRemoteImage.Local(R.drawable.ic_banner_upsell_card_reader_illustration)

    private fun Assets.getBadgeIcon() =
        this?.get(JITM_ASSETS_BADGE_IMAGE_KEY)?.let { JitmState.Banner.LabelOrRemoteIcon.Remote(it) }
            ?: JitmState.Banner.LabelOrRemoteIcon.Label(
                UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_new)
            )

    data class CtaClick(val url: String) : MultiLiveEvent.Event()

    companion object {
        const val JITM_MESSAGE_PATH_KEY = "jitm_message_path_key"
        private const val JITM_ASSETS_BACKGROUND_IMAGE_KEY = "background_image_url"
        private const val JITM_ASSETS_BADGE_IMAGE_KEY = "badge_image_url"

        private const val JITM_TEMPLATE_MODAL = "modal"
    }
}
