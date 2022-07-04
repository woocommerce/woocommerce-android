package com.woocommerce.android.support

import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatResult
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
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
    private val selectedSite: SelectedSite,
    networkStatus: NetworkStatus
) : ScopedViewModel(savedState) {
    companion object {
        const val EXIT_DELAY = 1000L
    }

    private val viewStateFlow = savedState.getStateFlow(
        viewModelScope,
        SSRViewState()
    )

    val viewState = viewStateFlow.asLiveData()

    init {
        if (networkStatus.isConnected()) {
            viewStateFlow.update { it.copy(isLoading = true) }
            launch(dispatchers.io) {
                val result = wooCommerceStore.fetchSSR(selectedSite.get())

                if (result.isError) {
                    withContext(dispatchers.main) {
                        triggerEvent(ShowSnackbar(R.string.support_system_status_report_fetch_error))
                        pauseThenExit()
                    }
                    return@launch
                }

                result.model?.let { ssr ->
                    withContext(dispatchers.main) {
                        viewStateFlow.update {
                            it.copy(
                                formattedSSR = ssr.formatResult(),
                                isLoading = false
                            )
                        }
                    }
                }
            }
        } else {
            triggerEvent(ShowSnackbar(R.string.offline_error))
            pauseThenExit()
        }
    }

    private fun pauseThenExit() {
        Handler(Looper.getMainLooper()).postDelayed({ triggerEvent(Exit) }, EXIT_DELAY)
    }

    fun onShareButtonClicked() {
        if (viewStateFlow.value.formattedSSR.isNotEmpty()) {
            triggerEvent(ShareSSR(viewStateFlow.value.formattedSSR))
        }
    }

    fun onCopyButtonClicked() {
        if (viewStateFlow.value.formattedSSR.isNotEmpty()) {
            triggerEvent(CopySSR(viewStateFlow.value.formattedSSR))
        }
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class SSRViewState(
        val formattedSSR: String = "",
        val isLoading: Boolean = false
    ) : Parcelable
}

data class CopySSR(val ssrText: String) : MultiLiveEvent.Event()
data class ShareSSR(val ssrText: String) : MultiLiveEvent.Event()
