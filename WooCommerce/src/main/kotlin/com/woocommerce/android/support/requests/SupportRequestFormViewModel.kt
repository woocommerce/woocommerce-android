package com.woocommerce.android.support.requests

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SupportRequestFormViewModel @Inject constructor(
    private val zendeskHelper: ZendeskHelper,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
}
