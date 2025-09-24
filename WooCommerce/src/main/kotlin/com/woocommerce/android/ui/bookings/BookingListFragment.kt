package com.woocommerce.android.ui.bookings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookingListFragment : TopLevelFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun getFragmentTitle() = getString(R.string.bookings_tab_title)
    override fun shouldExpandToolbar(): Boolean = false
    override fun scrollToTop() {
        return
    }

    private val viewModel: BookingListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            viewModel.state.observeAsState().value?.let { state ->
                Text("Empty Booking List screen: WIP")
            }
        }
    }
}
