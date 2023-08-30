package com.woocommerce.android.ui.login.storecreation.onboarding.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class GetPaidViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    private val args: GetPaidFragmentArgs by savedStateHandle.navArgs()

    private val setupUrl = selectedSite.get().url.slashJoin("/wp-admin/admin.php?page=wc-admin&task=${args.taskId}")

    val viewState = flowOf(
        ViewState(
            url = setupUrl,
            shouldAuthenticate = selectedSite.get().isWPComAtomic
        )
    ).asLiveData()

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    data class ViewState(
        val url: String,
        val shouldAuthenticate: Boolean
    )
}
