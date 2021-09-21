package com.woocommerce.android.support

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatResult
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class SSRActivityViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, SSRViewState())
    private var viewState by viewStateData

    init {
        viewState = viewState.copy(isLoading = true)
        launch(dispatchers.io) {
            val result = wooCommerceStore.fetchSSR(selectedSite.get())
            if (result.isError) {
                triggerEvent(ShowSnackbar(R.string.support_system_status_report_fetch_error))
                return@launch
            }

            result.model?.let {
                withContext(dispatchers.main) {
                    viewState = viewState.copy(formattedSSR = it.formatResult(), isLoading = false)
                }
            }
        }
    }

    fun onShareButtonTapped() {
        triggerEvent(ShareSSR(viewState.formattedSSR))
    }

    fun onCopyButtonTapped() {
        triggerEvent(CopySSR(viewState.formattedSSR))
    }

    @Parcelize
    data class SSRViewState(
        val formattedSSR: String = "",
        val isLoading: Boolean = false
    ) : Parcelable
}

data class CopySSR(val ssrText: String) : MultiLiveEvent.Event()
data class ShareSSR(val ssrText: String) : MultiLiveEvent.Event()
