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
import com.woocommerce.android.util.WooAnimUtils
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

    @Inject
    lateinit var selectedSite: SelectedSite

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

        // Temporary animation for testing the UI. Replace with actual animation when it's ready.
        WooAnimUtils.rotate(binding.secondStepIcon)

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
        val iconStart = R.drawable.ic_progress_circle_start
        val iconComplete = R.drawable.ic_progress_circle_complete
        when (status) {
            is Installing -> {
                binding.firstStepIcon.hide()
                setImageViews(iconStart, binding.secondStepIcon, binding.thirdStepIcon, binding.fourthStepIcon)

                // TODO Show loader on first step, hide all other loaders
            }
            is Activating -> {
                binding.firstStepIcon.show()
                binding.secondStepIcon.hide()
                setImageViews(iconComplete, binding.firstStepIcon)
                setImageViews(iconStart, binding.thirdStepIcon, binding.fourthStepIcon)

                // TODO Show loader on second step, hide all other loaders
            }
            is Connecting -> {
                binding.firstStepIcon.show()
                binding.secondStepIcon.show()
                binding.thirdStepIcon.hide()
                setImageViews(iconComplete, binding.firstStepIcon, binding.secondStepIcon)
                setImageViews(iconStart, binding.fourthStepIcon)

                // TODO Show loader on third step, hide all other loaders
            }
            is Finished -> {
                binding.firstStepIcon.show()
                binding.secondStepIcon.show()
                binding.thirdStepIcon.show()
                setImageViews(
                    iconComplete,
                    binding.firstStepIcon,
                    binding.secondStepIcon,
                    binding.thirdStepIcon,
                    binding.fourthStepIcon
                )

                // TODO Hide all loader

                binding.jetpackProgressActionButton.isEnabled = true
            }
            is Failed -> {
                // TODO Add error state
            }
        }
    }

    private fun setImageViews(resId: Int, vararg views: ImageView) = views.forEach { it.setImageResource(resId) }

    private fun isTabletLandscape() = (DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)) &&
        DisplayUtils.isLandscape(context)
}
