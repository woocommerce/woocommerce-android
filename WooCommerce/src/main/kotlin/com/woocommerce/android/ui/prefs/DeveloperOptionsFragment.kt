package com.woocommerce.android.ui.prefs

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentDeveloperOptionsBinding
import com.woocommerce.android.pos.PosActivity
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.UpdateOptions
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class DeveloperOptionsFragment : BaseFragment(R.layout.fragment_developer_options) {
    val viewModel: DeveloperOptionsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentDeveloperOptionsBinding.bind(view)

        initViews(binding)
        observeViewState(binding)
        observeEvents()
    }

    private fun initViews(binding: FragmentDeveloperOptionsBinding) {
        binding.developerOptionsRv.layoutManager = LinearLayoutManager(requireContext())
        binding.developerOptionsRv.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
        binding.developerOptionsRv.adapter = DeveloperOptionsAdapter()

        binding.posButton.setOnClickListener {
            startActivity(Intent(requireContext(), PosActivity::class.java))
        }
        binding.posButtonInternal.setOnClickListener {
            startActivity(Intent(requireContext(), PosActivity::class.java))
            val intent = Intent(requireContext(), PosActivity::class.java).apply {
                putExtra("screen_key", "pos_screen_two")
                putExtra("id", "12")
            }
            startActivity(intent)
        }
    }

    private fun observeEvents() {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is DeveloperOptionsViewModel.DeveloperOptionsEvents.ShowToastString -> {
                    ToastUtils.showToast(context, event.message)
                }

                is DeveloperOptionsViewModel.DeveloperOptionsEvents.ShowUpdateOptionsDialog -> {
                    showUpdateOptionsDialog(
                        values = event.options,
                        mapper = { requireContext().getString(it.title) },
                        selectedValue = event.selectedValue
                    )
                }
            }
        }
    }

    private fun showUpdateOptionsDialog(
        values: List<UpdateOptions>,
        mapper: (UpdateOptions) -> String,
        selectedValue: UpdateOptions
    ) {
        var currentlySelectedValue = selectedValue
        val textValues = values.map(mapper).toTypedArray()
        MaterialAlertDialogBuilder(
            ContextThemeWrapper(
                context,
                R.style.Theme_Woo_DayNight
            )
        )
            .setOnDismissListener {
                viewModel.onUpdateReaderOptionChanged(currentlySelectedValue)
            }
            .setSingleChoiceItems(textValues, selectedValue.ordinal) { _, which ->
                currentlySelectedValue = values[which]
            }.show()
    }

    private fun observeViewState(binding: FragmentDeveloperOptionsBinding) {
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            (binding.developerOptionsRv.adapter as DeveloperOptionsAdapter).setItems(state.rows)
        }
    }

    override fun getFragmentTitle() = resources.getString(R.string.dev_options)
}
