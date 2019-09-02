package com.woocommerce.android.ui.refunds

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject
import androidx.navigation.fragment.navArgs

class IssueRefundFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private lateinit var viewModel: IssueRefundViewModel

    private val navArgs: IssueRefundFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_refunds, container, false)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)

        initializeViewModel()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get(IssueRefundViewModel::class.java).also {
            setupObservers(it)
        }

        viewModel.start(navArgs.orderId)
    }

    override fun getFragmentTitle() = viewModel.formattedRefundAmount.value ?: ""

    private fun setupObservers(viewModel: IssueRefundViewModel) {
        viewModel.showSnackbarMessage.observe(this, Observer {
            uiMessageResolver.showSnack(it)
        })

        viewModel.exit.observe(this, Observer {
            activity?.onBackPressed()
        })

        viewModel.formattedRefundAmount.observe(this, Observer {
            activity?.title = it
        })
    }
}
