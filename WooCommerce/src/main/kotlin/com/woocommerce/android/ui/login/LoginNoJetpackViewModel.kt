package com.woocommerce.android.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.launch

class LoginNoJetpackViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val loginNoJetpackRepository: LoginNoJetpackRepository
) : ScopedViewModel(savedState, dispatchers) {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isJetpackAvailable = MutableLiveData<Boolean>()
    val isJetpackAvailable: LiveData<Boolean> = _isJetpackAvailable

    fun verifyJetpackAvailable(siteAddress: String) {
        launch {
            _isLoading.value = true
            _isJetpackAvailable.value = loginNoJetpackRepository.verifyJetpackAvailable(siteAddress)
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        loginNoJetpackRepository.onCleanup()
    }

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<LoginNoJetpackViewModel>
}
