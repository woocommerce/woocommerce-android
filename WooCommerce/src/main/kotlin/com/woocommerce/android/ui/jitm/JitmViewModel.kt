package com.woocommerce.android.ui.jitm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import javax.inject.Inject

private typealias Assets = Map<String, String>?

@HiltViewModel
class JitmViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val jitmStoreCache: JitmStoreInMemoryCache,
    private val jitmTracker: JitmTracker,
    private val jitmUtmProvider: JitmUtmProvider,
    private val selectedSite: SelectedSite,
) : ScopedViewModel(savedState) {
    private val _jitmState: MutableLiveData<JitmState> = MutableLiveData()
    val jitmState: LiveData<JitmState> = _jitmState

    private val messagePath = savedState.get<String>(JITM_MESSAGE_PATH_KEY)!!
    private val utmSource = messagePath.split(":")[1]

    init {
        fetchJitms()
    }

    fun fetchJitms() {
        fetchJitms(savedState.get<String>(JITM_MESSAGE_PATH_KEY)!!)
    }

    private fun fetchJitms(jitmMessagePath: String) {
        launch {
            val messages = jitmStoreCache.getMessagesForPath(jitmMessagePath)
            populateResultToUI(messages.firstOrNull())
        }
    }

    private fun populateResultToUI(response: JITMApiResponse?) {
        response?.let { model: JITMApiResponse ->
            jitmTracker.trackJitmDisplayed(
                utmSource,
                model.id,
                model.featureClass
            )

            _jitmState.value = when (model.template) {
                JITM_TEMPLATE_MODAL -> JitmState.Modal(
                    onPrimaryActionClicked = { onJitmCtaClicked(model) },
                    onDismissClicked = { onJitmDismissClicked(model) },
                    title = UiString.UiStringText(model.content.message),
                    description = UiString.UiStringText(model.content.description),
                    primaryActionLabel = UiString.UiStringText(model.cta.message),
                    backgroundLightImageUrl = model.assets?.get(JITM_ASSETS_BACKGROUND_IMAGE_LIGHT_THEME_KEY),
                    backgroundDarkImageUrl = model.assets?.get(JITM_ASSETS_BACKGROUND_IMAGE_DARK_THEME_KEY)
                        ?: model.assets?.get(JITM_ASSETS_BACKGROUND_IMAGE_LIGHT_THEME_KEY),
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
        jitmStoreCache.onCtaClicked(messagePath)
        jitmTracker.trackJitmCtaTapped(
            utmSource,
            model.id,
            model.featureClass
        )
        triggerEvent(
            CtaClick(
                jitmUtmProvider.getUrlWithUtmParams(
                    source = utmSource,
                    id = model.id,
                    featureClass = model.featureClass,
                    siteId = selectedSite.getIfExists()?.siteId,
                    url = model.cta.link
                )
            )
        )
    }

    private fun onJitmDismissClicked(model: JITMApiResponse) {
        _jitmState.value = JitmState.Hidden
        jitmTracker.trackJitmDismissTapped(utmSource, model.id, model.featureClass)
        launch {
            jitmStoreCache.dismissJitmMessage(messagePath, model.id, model.featureClass).also { response ->
                when {
                    response.model != null && response.model!! -> {
                        jitmTracker.trackJitmDismissSuccess(
                            utmSource,
                            model.id,
                            model.featureClass
                        )
                    }

                    else -> jitmTracker.trackJitmDismissFailure(
                        utmSource,
                        model.id,
                        model.featureClass,
                        response.error?.type,
                        response.error?.message
                    )
                }
            }
        }
    }

    private fun Assets.getBackgroundImage() =
        this?.get(JITM_ASSETS_BACKGROUND_IMAGE_LIGHT_THEME_KEY)?.let {
            JitmState.Banner.LocalOrRemoteImage.Remote(
                urlLightMode = it,
                urlDarkMode = this[JITM_ASSETS_BACKGROUND_IMAGE_DARK_THEME_KEY] ?: it
            )
        } ?: JitmState.Banner.LocalOrRemoteImage.Local(R.drawable.ic_banner_upsell_card_reader_illustration)

    private fun Assets.getBadgeIcon() =
        this?.get(JITM_ASSETS_BADGE_IMAGE_LIGHT_THEME_KEY)?.let {
            JitmState.Banner.LabelOrRemoteIcon.Remote(
                urlLightMode = it,
                urlDarkMode = this[JITM_ASSETS_BADGE_IMAGE_DARK_THEME_KEY] ?: it
            )
        }
            ?: JitmState.Banner.LabelOrRemoteIcon.Label(
                UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_new)
            )

    data class CtaClick(val url: String) : MultiLiveEvent.Event()

    companion object {
        const val JITM_MESSAGE_PATH_KEY = "jitm_message_path_key"

        private const val JITM_ASSETS_BACKGROUND_IMAGE_LIGHT_THEME_KEY = "background_image_url"
        private const val JITM_ASSETS_BACKGROUND_IMAGE_DARK_THEME_KEY = "background_image_dark_url"

        private const val JITM_ASSETS_BADGE_IMAGE_LIGHT_THEME_KEY = "badge_image_url"
        private const val JITM_ASSETS_BADGE_IMAGE_DARK_THEME_KEY = "badge_image_dark_url"

        private const val JITM_TEMPLATE_MODAL = "modal"
    }
}
