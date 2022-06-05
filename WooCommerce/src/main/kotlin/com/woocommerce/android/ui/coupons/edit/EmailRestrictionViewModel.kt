package com.woocommerce.android.ui.coupons.edit

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EmailRestrictionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {

    fun onBackPressed() {
        /* TODO */
    }

    data class ViewState(
        val emailRestrictions: List<String>,
    )
}
