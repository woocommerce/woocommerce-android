package com.woocommerce.android.support

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
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
        viewState = viewState.copy(
            exampleString = exampleStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
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
