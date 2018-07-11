package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.fluxc.model.WCOrderModel
import javax.inject.Inject

class OrderDetailAddNoteFragment : Fragment(), OrderDetailAddNoteContract.View {
    companion object {
        const val TAG = "OrderDetailAddNoteFragment"
        const val FIELD_ORDER_IDENTIFIER = "order-identifier"

        fun newInstance(order: WCOrderModel): Fragment {
            val args = Bundle()
            args.putString(FIELD_ORDER_IDENTIFIER, order.getIdentifier())
            val fragment = OrderDetailAddNoteFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var presenter: OrderDetailAddNoteContract.Presenter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_order_detail_add_note, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }
}
