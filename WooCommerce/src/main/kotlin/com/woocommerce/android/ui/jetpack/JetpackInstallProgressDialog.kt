package com.woocommerce.android.ui.jetpack

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogJetpackInstallProgressBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.InstallStatus
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.InstallStatus.*
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class JetpackInstallProgressDialog : DialogFragment(R.layout.dialog_jetpack_install_progress) {
    companion object {
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.35f
        private const val TABLET_LANDSCAPE_HEIGHT_RATIO = 0.8f
    }

    @Inject lateinit var selectedSite: SelectedSite

    private val viewModel: JetpackInstallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use fullscreen style for all cases except tablet in landscape mode
        setStyle(STYLE_NO_TITLE, if (isTabletLandscape()) R.style.Theme_Woo_Dialog else R.style.Theme_Woo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Specify transition animations
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog

        val binding = DialogJetpackInstallProgressBinding.bind(view)

        with(binding.subtitle) {
            val stringBuilder = StringBuilder()
            stringBuilder.append(context.getString(R.string.jetpack_install_start_default_name))

            selectedSite.get().name?.let {
                stringBuilder.append(" <b>${selectedSite.get().name}</b> ")
            }

            text = HtmlCompat.fromHtml(
                context.getString(R.string.jetpack_install_progress_subtitle, stringBuilder.toString()),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }

        binding.jetpackProgressActionButton.setOnClickListener {
            findNavController().navigateSafely(
                JetpackInstallProgressDialogDirections.actionJetpackInstallProgressDialogToDashboard()
            )
        }

        viewModel.installJetpackPlugin()

        setupObservers(binding)
    }

    override fun onStart() {
        super.onStart()
        if (isTabletLandscape()) {
            requireDialog().window!!.setLayout(
                (DisplayUtils.getDisplayPixelWidth() * TABLET_LANDSCAPE_WIDTH_RATIO).toInt(),
                (DisplayUtils.getDisplayPixelHeight(context) * TABLET_LANDSCAPE_HEIGHT_RATIO).toInt()
            )
        }
    }

    private fun setupObservers(binding: DialogJetpackInstallProgressBinding) {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.installStatus?.takeIfNotEqualTo(old?.installStatus) {
                updateInstallProgressUi(it, binding)
            }
        }
    }

    private fun updateInstallProgressUi(status: InstallStatus, binding: DialogJetpackInstallProgressBinding) {
        val iconNotDone = R.drawable.ic_progress_circle_start
        val iconDone = R.drawable.ic_progress_circle_complete
        val iconStep1 = binding.firstStepIcon
        val iconStep2 = binding.secondStepIcon
        val iconStep3 = binding.thirdStepIcon
        val iconStep4 = binding.fourthStepIcon
        val progressStep1 = binding.firstStepProgressBar
        val progressStep2 = binding.secondStepProgressBar
        val progressStep3 = binding.thirdStepProgressBar

        when (status) {
            is Installing -> {
                setViewsVisibility(View.INVISIBLE, iconStep1, progressStep2, progressStep3)
                setViewsVisibility(View.VISIBLE, iconStep2, iconStep3, iconStep4, progressStep1)
                setImageViews(iconNotDone, iconStep2, iconStep3, iconStep4)

                binding.jetpackProgressActionButton.hide()
            }
            is Activating -> {
                setViewsVisibility(View.INVISIBLE, iconStep2, progressStep1, progressStep3)
                setViewsVisibility(View.VISIBLE, iconStep1, iconStep3, iconStep4, progressStep2)
                setImageViews(iconNotDone, iconStep3, iconStep4)
                setImageViews(iconDone, iconStep1)

                binding.jetpackProgressActionButton.hide()
            }
            is Connecting -> {
                setViewsVisibility(View.INVISIBLE, iconStep3, progressStep1, progressStep2)
                setViewsVisibility(View.VISIBLE, iconStep1, iconStep2, iconStep4, progressStep3)
                setImageViews(iconNotDone, iconStep4)
                setImageViews(iconDone, iconStep1, iconStep2)

                binding.jetpackProgressActionButton.hide()
            }
            is Finished -> {
                setViewsVisibility(View.INVISIBLE, progressStep1, progressStep2, progressStep3)
                setViewsVisibility(View.VISIBLE, iconStep1, iconStep2, iconStep3, iconStep4)
                setImageViews(iconDone, iconStep1, iconStep2, iconStep3, iconStep4)

                binding.jetpackProgressActionButton.show()
            }
            is Failed -> {
                // TODO Add error state
            }
        }
    }

    private fun setViewsVisibility(visibility: Int, vararg views: View) = views.forEach { it.visibility = visibility }
    private fun setImageViews(resId: Int, vararg views: ImageView) = views.forEach { it.setImageResource(resId) }

    private fun isTabletLandscape() = (DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)) &&
        DisplayUtils.isLandscape(context)
}
