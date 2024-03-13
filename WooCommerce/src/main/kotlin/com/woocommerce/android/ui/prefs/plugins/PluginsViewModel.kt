package com.woocommerce.android.ui.prefs.plugins

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loaded.Plugin
import com.woocommerce.android.ui.prefs.plugins.PluginsViewModel.ViewState.Loading
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class PluginsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    wooStore: WooCommerceStore,
    site: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow<ViewState>(Loading)
    val viewState = _viewState.asLiveData()

    init {
        viewModelScope.launch {
            val result = wooStore.fetchSitePlugins(site.get())
            if (result.isError || result.model == null) {
                _viewState.value = ViewState.Error
            } else {
                _viewState.value = ViewState.Loaded(
                    plugins = result.model!!
                        .filter { it.version.isNotNullOrEmpty() }
                        .map { Plugin(it.displayName, it.version) }
                )
            }
        }
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    sealed interface ViewState {
        data object Loading : ViewState
        data object Error : ViewState
        data class Loaded(
            val plugins: List<Plugin> = emptyList()
        ) : ViewState {
            data class Plugin(
                val name: String,
                val version: String
            )
        }
    }
}
