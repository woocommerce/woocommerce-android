package com.woocommerce.android.ui.prefs.cardreader.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogCardReaderUpdateBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderUpdateDialogFragment : DialogFragment(R.layout.dialog_card_reader_update) {
    val viewModel: CardReaderUpdateViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog!!.setCanceledOnTouchOutside(false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = DialogCardReaderUpdateBinding.bind(view)

        initObservers(binding)
    }

    private fun initObservers(binding: DialogCardReaderUpdateBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner, { state ->

        })
    }

    companion object {
        internal const val KEY_SKIP_UPDATE = "key_skip_update"

        fun newInstance(skipUpdate: Boolean) =
            CardReaderUpdateDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(KEY_SKIP_UPDATE, skipUpdate)
                }
            }
    }
}
