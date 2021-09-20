package com.woocommerce.android.support

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatResult
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.wordpress.android.fluxc.model.WCSSRModel
import javax.inject.Inject

@HiltViewModel
class SSRActivityViewModel @Inject constructor(
    savedState: SavedStateHandle,
    resourceProvider: ResourceProvider
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, SSRViewState())
    private var viewState by viewStateData

    init {
        val exampleStream = resourceProvider.openRawResource(R.raw.system_status)
        val exampleJSON = JSONObject(exampleStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        val exampleModel = WCSSRModel(
            remoteSiteId = 1,
            environment = exampleJSON["environment"].toString(),
            database = exampleJSON["database"].toString(),
            activePlugins = exampleJSON["active_plugins"].toString(),
            theme = exampleJSON["theme"].toString(),
            settings = exampleJSON["settings"].toString(),
            security = exampleJSON["security"].toString(),
            pages = exampleJSON["pages"].toString()
        )

        viewState = viewState.copy(
            exampleString = exampleModel.formatResult()
        )
    }

    fun onShareButtonTapped() {
        triggerEvent(ShareSSR(viewState.exampleString))
    }

    fun onCopyButtonTapped() {
        triggerEvent(CopySSR(viewState.exampleString))
    }

    @Parcelize
    data class SSRViewState(
        val exampleString: String = ""
    ) : Parcelable
}

data class CopySSR(val ssrText: String) : MultiLiveEvent.Event()
data class ShareSSR(val ssrText: String) : MultiLiveEvent.Event()
