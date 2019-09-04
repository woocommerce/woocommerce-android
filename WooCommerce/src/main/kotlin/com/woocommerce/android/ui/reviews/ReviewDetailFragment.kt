package com.woocommerce.android.ui.reviews

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

class ReviewDetailFragment : BaseFragment() {
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var productImageMap: ProductImageMap

    private var runOnStartFunc: (() -> Unit)? = null
    private var productIconSize: Int = 0

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_review_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val dimen = activity!!.resources.getDimensionPixelSize(R.dimen.product_icon_sz)
        productIconSize = DisplayUtils.dpToPx(activity, dimen)
    }

    override fun getFragmentTitle() = getString(R.string.wc_review_title)
}
