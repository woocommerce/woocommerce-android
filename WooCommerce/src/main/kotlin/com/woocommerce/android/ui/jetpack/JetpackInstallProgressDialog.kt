package com.woocommerce.android.ui.jetpack

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogJetpackInstallProgressBinding
import com.woocommerce.android.extensions.*
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.InstallStatus
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.InstallStatus.*
import com.woocommerce.android.ui.jetpack.JetpackInstallViewModel.FailureType.*
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class JetpackInstallProgressDialog : DialogFragment(R.layout.dialog_jetpack_install_progress) {
    companion object {
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.35f
        private const val TABLET_LANDSCAPE_HEIGHT_RATIO = 0.8f
        private const val JETPACK_INSTALL_URL = "plugin-install.php?tab=plugin-information&plugin=jetpack"
        private const val JETPACK_ACTIVATE_URL = "plugins.php"
        private const val ICON_NOT_DONE = R.drawable.ic_progress_circle_start
        private const val ICON_DONE = R.drawable.ic_progress_circle_complete
    }

    @Inject lateinit var selectedSite: SelectedSite

    private val viewModel: JetpackInstallViewModel by viewModels()

    private lateinit var iconStep1: ImageView
    private lateinit var iconStep2: ImageView
    private lateinit var iconStep3: ImageView
    private lateinit var iconStep4: ImageView
    private lateinit var progressStep1: ProgressBar
    private lateinit var progressStep2: ProgressBar
    private lateinit var progressStep3: ProgressBar
    private lateinit var messageStep1: MaterialTextView
    private lateinit var messageStep2: MaterialTextView
    private lateinit var messageStep3: MaterialTextView
    private lateinit var messageStep4: MaterialTextView

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

        with(binding) {
            iconStep1 = firstStepIcon
            iconStep2 = secondStepIcon
            iconStep3 = thirdStepIcon
            iconStep4 = fourthStepIcon
            progressStep1 = firstStepProgressBar
            progressStep2 = secondStepProgressBar
            progressStep3 = thirdStepProgressBar
            messageStep1 = firstStepMessage
            messageStep2 = secondStepMessage
            messageStep3 = thirdStepMessage
            messageStep4 = fourthStepMessage
        }

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

        binding.contactButton.setOnClickListener {
            activity?.startHelpActivity(HelpActivity.Origin.JETPACK_INSTALLATION)
        }

        setupObservers(binding)
    }

    override fun onStart() {
        super.onStart()
        if (isTabletLandscape()) {
            requireDialog().window!!.setLayout(
                (DisplayUtils.getWindowPixelWidth(requireContext()) * TABLET_LANDSCAPE_WIDTH_RATIO).toInt(),
                (DisplayUtils.getWindowPixelHeight(requireContext()) * TABLET_LANDSCAPE_HEIGHT_RATIO).toInt()
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
        when (status) {
            is Installing -> {
                setViewVisibility(View.INVISIBLE, iconStep1, progressStep2, progressStep3)
                setViewVisibility(View.VISIBLE, iconStep2, iconStep3, iconStep4, progressStep1)
                setViewImage(ICON_NOT_DONE, iconStep2, iconStep3, iconStep4)
                setTextWeight(Typeface.BOLD, messageStep1)
                setTextWeight(Typeface.NORMAL, messageStep2, messageStep3, messageStep4)

                binding.jetpackProgressActionButton.hide()
            }
            is Activating -> {
                setViewVisibility(View.INVISIBLE, iconStep2, progressStep1, progressStep3)
                setViewVisibility(View.VISIBLE, iconStep1, iconStep3, iconStep4, progressStep2)
                setViewImage(ICON_NOT_DONE, iconStep3, iconStep4)
                setViewImage(ICON_DONE, iconStep1)
                setTextWeight(Typeface.BOLD, messageStep1, messageStep2)
                setTextWeight(Typeface.NORMAL, messageStep3, messageStep4)

                binding.jetpackProgressActionButton.hide()
            }
            is Connecting -> {
                setViewVisibility(View.INVISIBLE, iconStep3, progressStep1, progressStep2)
                setViewVisibility(View.VISIBLE, iconStep1, iconStep2, iconStep4, progressStep3)
                setViewImage(ICON_NOT_DONE, iconStep4)
                setViewImage(ICON_DONE, iconStep1, iconStep2)
                setTextWeight(Typeface.BOLD, messageStep1, messageStep2, messageStep3)
                setTextWeight(Typeface.NORMAL, messageStep4)

                binding.jetpackProgressActionButton.hide()
            }
            is Finished -> {
                setViewVisibility(View.INVISIBLE, progressStep1, progressStep2, progressStep3)
                setViewVisibility(View.VISIBLE, iconStep1, iconStep2, iconStep3, iconStep4)
                setViewImage(ICON_DONE, iconStep1, iconStep2, iconStep3, iconStep4)
                setTextWeight(Typeface.BOLD, messageStep1, messageStep2, messageStep3, messageStep4)

                binding.jetpackProgressActionButton.show()
            }
            is Failed -> {
                handleFailedState(status, binding)
                handleWpAdminButton(status.errorType, binding.openAdminOrRetryButton)
            }
        }
    }

    private fun handleFailedState(status: Failed, binding: DialogJetpackInstallProgressBinding) {
        val ctx = binding.root.context

        // Title copy
        binding.title.text = ctx.getString(
            R.string.jetpack_install_progress_failed_title,
            when (status.errorType) {
                INSTALLATION -> ctx.getString(R.string.jetpack_install_progress_failed_reason_installation)
                ACTIVATION -> ctx.getString(R.string.jetpack_install_progress_failed_reason_activation)
                CONNECTION -> ctx.getString(R.string.jetpack_install_progress_failed_reason_connection)
            }
        )

        // Subtitle copy
        val subtitle = ctx.getString(R.string.jetpack_install_progress_failed_try)
        val sb = StringBuilder()
        sb.append(subtitle)
        if (status.errorType != CONNECTION) {
            sb.append(" ")
            sb.append(
                when (status.errorType) {
                    INSTALLATION -> ctx.getString(R.string.jetpack_install_progress_failed_alternative_install)
                    ACTIVATION -> ctx.getString(R.string.jetpack_install_progress_failed_alternative_activate)
                    else -> ""
                }
            )
        }
        binding.subtitle.text = sb.toString()

        // Button copy
        val btnText = when (status.errorType) {
            INSTALLATION -> {
                ctx.getString(
                    R.string.jetpack_install_progress_failed_option_wp_admin,
                    ctx.getString(R.string.jetpack_install_progress_option_wp_admin_install)
                )
            }
            ACTIVATION -> {
                ctx.getString(
                    R.string.jetpack_install_progress_failed_option_wp_admin,
                    ctx.getString(R.string.jetpack_install_progress_option_wp_admin_activate)
                )
            }
            CONNECTION -> ctx.getString(R.string.try_again)
        }
        binding.openAdminOrRetryButton.text = btnText

        // Visibilities
        setViewVisibility(View.GONE, iconStep1, iconStep2, iconStep3, iconStep4)
        setViewVisibility(View.GONE, messageStep1, messageStep2, messageStep3, messageStep4)
        setViewVisibility(View.GONE, progressStep1, progressStep2, progressStep3)
        binding.contactButton.show()
        binding.openAdminOrRetryButton.show()
        binding.jetpackProgressActionButton.hide()
    }
    private fun handleWpAdminButton(errorType: JetpackInstallViewModel.FailureType, button: MaterialButton) {
        when (errorType) {
            INSTALLATION -> {
                button.setOnClickListener {
                    val installJetpackInWpAdminUrl = selectedSite.get().adminUrl + JETPACK_INSTALL_URL
                    ChromeCustomTabUtils.launchUrl(requireContext(), installJetpackInWpAdminUrl)
                }
            }
            ACTIVATION -> {
                button.setOnClickListener {
                    val activateJetpackInWpAdminUrl = selectedSite.get().adminUrl + JETPACK_ACTIVATE_URL
                    ChromeCustomTabUtils.launchUrl(requireContext(), activateJetpackInWpAdminUrl)
                }
            }
            else -> {
                // Add sync functionality.
            }
        }
    }

    private fun setViewVisibility(visibility: Int, vararg views: View) = views.forEach { it.visibility = visibility }
    private fun setViewImage(resId: Int, vararg views: ImageView) = views.forEach { it.setImageResource(resId) }
    private fun setTextWeight(weight: Int, vararg views: TextView) = views.forEach { it.setTypeface(null, weight) }

    private fun isTabletLandscape() = (DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)) &&
        DisplayUtils.isLandscape(context)
}
