package com.woocommerce.android.ui.login

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.navigation.NavHostController
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.woocommerce.android.ui.NavRoutes.MY_STORE
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.Parcelize

@HiltViewModel(assistedFactory = LoginViewModel.Factory::class)
class LoginViewModel @AssistedInject constructor(
    private val loginRepository: LoginRepository,
    private val messageClient: MessageClient,
    private val capabilityClient: CapabilityClient,
    @Assisted private val navController: NavHostController,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        launch { observeLoginChanges() }
    }

    private suspend fun observeLoginChanges() {
        loginRepository.isUserLoggedIn().collect { isLoggedIn ->
            if (isLoggedIn) {
                navController.navigate(MY_STORE.route)
            } else {
                _viewState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onLoginButtonClicked() {
        _viewState.update { it.copy(isLoading = true) }
        launch {
            fetchReachableNodes()
                .map { (node, _) ->
                    async {
                        messageClient.sendMessage(node.id, "/request_auth", byteArrayOf())
                    }
                }.awaitAll()

            _viewState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun fetchReachableNodes() = capabilityClient
        .getAllCapabilities(CapabilityClient.FILTER_REACHABLE)
        .await()
        .flatMap { (capability, capabilityInfo) ->
            capabilityInfo.nodes.map { it to capability }
        }
        .groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )
        .mapValues { it.value.toSet() }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = true,
        val isSyncButtonVisible: Boolean = false
    ) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(navController: NavHostController): LoginViewModel
    }
}
