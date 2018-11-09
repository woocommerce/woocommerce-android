package com.woocommerce.android.ui.notifications

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.notifications.ReviewDetailContract.View
import org.wordpress.android.fluxc.Dispatcher
import javax.inject.Inject

class ReviewDetailPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite
) : ReviewDetailContract.Presenter {
    companion object {
        private val TAG: String = ReviewDetailPresenter::class.java.simpleName
    }

    private var detailView: ReviewDetailContract.View? = null

    override fun takeView(view: View) {
        detailView = view
    }

    override fun dropView() {
        detailView = null
    }
}
