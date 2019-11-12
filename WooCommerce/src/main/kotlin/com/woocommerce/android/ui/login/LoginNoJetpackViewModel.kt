package com.woocommerce.android.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class LoginNoJetpackViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val loginNoJetpackRepository: LoginNoJetpackRepository
) : ScopedViewModel(mainDispatcher) {
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
}
