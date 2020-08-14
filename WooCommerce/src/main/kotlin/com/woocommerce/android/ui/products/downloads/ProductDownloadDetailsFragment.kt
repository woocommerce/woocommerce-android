package com.woocommerce.android.ui.products.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_product_download_details.*
import javax.inject.Inject

class ProductDownloadDetailsFragment : BaseFragment(), BackPressListener {
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: ProductDownloadDetailsViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_download_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    private fun setupObservers(viewModel: ProductDownloadDetailsViewModel) {
        viewModel.productDownloadDetailsViewStateData.observe(owner = viewLifecycleOwner, observer = { old, new ->
            new.fileDraft.url.takeIfNotEqualTo(product_download_url.getText()) {
                product_download_url.setText(it)
            }
            new.fileDraft.name.takeIfNotEqualTo(product_download_name.getText()) {
                product_download_name.setText(it)
            }

            showDoneMenuItem(new.changesMade)
        })

        initListeners()
    }

    private fun initListeners() {
        product_download_url.setOnTextChangedListener {
            viewModel.onFileUrlChanged(it.toString())
        }
        product_download_name.setOnTextChangedListener {
            viewModel.onFileNameChanged(it.toString())
        }
    }

    private fun showDoneMenuItem(show: Boolean) {
        // doneMenuItem?.isVisible = show
    }

    override fun getFragmentTitle(): String {
        return super.getFragmentTitle()
    }

    override fun onRequestAllowBackPress(): Boolean {
        // TODO check if details were modified
        return true
    }
}
