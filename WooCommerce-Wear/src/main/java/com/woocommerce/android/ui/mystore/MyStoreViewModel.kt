package com.woocommerce.android.ui.mystore

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.commons.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = MyStoreViewModel.Factory::class)
class MyStoreViewModel @AssistedInject constructor(
    private val loginRepository: LoginRepository,
    @Assisted private val navController: NavHostController,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    @AssistedFactory
    interface Factory {
        fun create(navController: NavHostController): MyStoreViewModel
    }
}
