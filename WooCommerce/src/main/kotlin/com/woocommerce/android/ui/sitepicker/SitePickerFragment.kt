package com.woocommerce.android.ui.sitepicker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.AppUrls
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSitePickerBinding
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.navigateToHelpScreen
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.support.help.HelpActivity
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragment
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.LoginEmailHelpDialogFragment
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorFragment
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.AccountMismatchErrorType
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToAccountMismatchScreen
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToAddStoreEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToEmailHelpDialogEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToHelpFragmentEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToMainActivityEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToNewToWooEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.NavigateToWPComWebView
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerEvent.ShowWooUpgradeDialogEvent
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.NoStoreState
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.SimpleWPComState
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.StoreListState
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitePickerState.WooNotFoundState
import com.woocommerce.android.ui.sitepicker.sitediscovery.SitePickerSiteDiscoveryFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

@AndroidEntryPoint
class SitePickerFragment :
    BaseFragment(R.layout.fragment_site_picker),
    LoginEmailHelpDialogFragment.Listener,
    MenuProvider {
    private var _binding: FragmentSitePickerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SitePickerViewModel by viewModels()
    private val navArgs by navArgs<SitePickerFragmentArgs>()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private var skeletonView = SkeletonView()
    private var progressDialog: CustomProgressDialog? = null

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = if (!navArgs.openedFromLogin) R.drawable.ic_back_24dp else null,
            hasShadow = false,
            hasDivider = false,
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentSitePickerBinding.bind(view)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        setupViews()
        setupObservers(viewModel)
        handleResults()
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_site_picker, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onHelpButtonClick()
                true
            }

            R.id.menu_close_account -> {
                findNavController().navigateSafely(
                    SitePickerFragmentDirections.actionSitePickerFragmentToCloseAccountDialogFragment()
                )
                true
            }

            else -> false
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.menu_help).isVisible =
            viewModel.sitePickerViewStateData.liveData.value?.isHelpBtnVisible ?: false
        menu.findItem(R.id.menu_close_account).isVisible =
            viewModel.sitePickerViewStateData.liveData.value?.showCloseAccountMenuItem ?: false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViews() {
        binding.sitesRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = SitePickerAdapter(viewModel::onSiteSelected, viewModel::onNonWooSiteSelected)
        }
        binding.loginEpilogueButtonBar.buttonSecondary.setOnClickListener {
            viewModel.onTryAnotherAccountButtonClick()
        }

        binding.addStoreButton.setOnClickListener {
            viewModel.onAddStoreClick()
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun setupObservers(viewModel: SitePickerViewModel) {
        viewModel.sitePickerViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.userInfo?.takeIfNotEqualTo(old?.userInfo) {
                updateUserInfoView(it)
            }
            new.isPrimaryBtnVisible.takeIfNotEqualTo(old?.isPrimaryBtnVisible) {
                binding.loginEpilogueButtonBar.buttonPrimary.isVisible = it
            }
            new.isSecondaryBtnVisible.takeIfNotEqualTo(old?.isSecondaryBtnVisible) {
                binding.loginEpilogueButtonBar.buttonSecondary.isVisible = it
            }
            new.isNoStoresViewVisible.takeIfNotEqualTo(old?.isNoStoresViewVisible) {
                binding.sitesRecycler.isVisible = !it
                binding.addStoreButton.isVisible = !it
                binding.noStoresView.isVisible = it
            }
            new.toolbarTitle?.takeIfNotEqualTo(old?.toolbarTitle) {
                activity?.title = it
            }
            new.primaryBtnText?.takeIfNotEqualTo(old?.primaryBtnText) {
                binding.loginEpilogueButtonBar.buttonPrimary.text = it
            }
            new.secondaryBtnText?.takeIfNotEqualTo(old?.secondaryBtnText) {
                binding.loginEpilogueButtonBar.buttonSecondary.text = it
            }
            new.noStoresBtnText?.takeIfNotEqualTo(old?.noStoresBtnText) {
                binding.noStoresView.noStoresBtnText = it
            }
            new.isNoStoresBtnVisible.takeIfNotEqualTo(old?.isNoStoresBtnVisible) {
                binding.noStoresView.isNoStoresBtnVisible = it
            }
            new.noStoresLabelText.takeIfNotEqualTo(old?.noStoresLabelText) {
                binding.noStoresView.noStoresText = it.orEmpty()
            }
            new.noStoresSubText.takeIfNotEqualTo(old?.noStoresSubText) {
                binding.noStoresView.noStoresSubtext = it.orEmpty()
            }
            new.isSkeletonViewVisible.takeIfNotEqualTo(old?.isSkeletonViewVisible) {
                updateSkeletonView(it)
            }
            new.isProgressDiaLogVisible.takeIfNotEqualTo(old?.isProgressDiaLogVisible) {
                showProgressDialog(it)
            }
            new.currentSitePickerState.takeIfNotEqualTo(old?.currentSitePickerState) {
                when (it) {
                    StoreListState -> updateStoreListView()
                    NoStoreState -> updateNoStoresView()
                    WooNotFoundState -> updateWooNotFoundView()
                    SimpleWPComState -> updateSimpleWPComView()
                }
            }
        }

        viewModel.sites.observe(viewLifecycleOwner) {
            (binding.sitesRecycler.adapter as? SitePickerAdapter)?.submitList(it)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigateToMainActivityEvent -> (activity as? MainActivity)?.handleSitePickerResult()
                is ShowWooUpgradeDialogEvent -> showWooUpgradeDialog()
                is NavigateToHelpFragmentEvent -> navigateToHelpScreen(event.origin)
                is NavigateToNewToWooEvent -> navigateToNewToWooScreen()
                is NavigateToAddStoreEvent -> navigateToAddStoreScreen()
                is NavigateToEmailHelpDialogEvent -> navigateToNeedHelpFindingEmailScreen()
                is NavigateToWPComWebView -> navigateToWPComWebView(event)
                is NavigateToAccountMismatchScreen -> navigateToAccountMismatchScreen(event)
                is ShowSnackbar -> uiMessageResolver.getSnack(stringResId = event.message, stringArgs = event.args)
                    .show()

                is ShowDialog -> event.showDialog()
                is Logout -> onLogout()
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
    }

    private fun handleResults() {
        handleNotice(WPComWebViewFragment.WEBVIEW_RESULT) {
            AnalyticsTracker.track(AnalyticsEvent.LOGIN_WOOCOMMERCE_SETUP_COMPLETED)
            viewModel.onWooInstalled()
        }
        handleNotice(WPComWebViewFragment.WEBVIEW_DISMISSED) {
            AnalyticsTracker.track(AnalyticsEvent.LOGIN_WOOCOMMERCE_SETUP_DISMISSED)
        }
        handleResult<String>(SitePickerSiteDiscoveryFragment.SITE_PICKER_SITE_ADDRESS_RESULT) {
            viewModel.onSiteAddressReceived(it)
        }
        handleNotice(AccountMismatchErrorFragment.JETPACK_CONNECTED_NOTICE) {
            viewModel.onJetpackConnected()
        }
    }

    private fun updateStoreListView() {
        binding.loginEpilogueButtonBar.buttonPrimary.setOnClickListener {
            viewModel.onContinueButtonClick()
        }
    }

    private fun updateNoStoresView() {
        binding.noStoresView.illustration = AppCompatResources.getDrawable(
            requireActivity(),
            R.drawable.img_site_picker_no_stores
        )
        binding.loginEpilogueButtonBar.buttonPrimary.setOnClickListener {
            viewModel.onAddStoreClick()
        }
        binding.noStoresView.clickSecondaryAction {
            viewModel.onNewToWooClick()
        }
    }

    private fun updateWooNotFoundView() {
        binding.noStoresView.illustration = AppCompatResources.getDrawable(
            requireActivity(),
            R.drawable.img_woo_generic_error
        )
        binding.loginEpilogueButtonBar.buttonPrimary.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.LOGIN_WOOCOMMERCE_SETUP_BUTTON_TAPPED)
            viewModel.onInstallWooClicked()
        }
        binding.noStoresView.clickSecondaryAction {
            viewModel.onViewConnectedStoresButtonClick()
        }
    }

    private fun updateSimpleWPComView() {
        binding.noStoresView.illustration = AppCompatResources.getDrawable(
            requireActivity(),
            R.drawable.img_woo_generic_error
        )
        binding.loginEpilogueButtonBar.buttonPrimary.setOnClickListener {
            viewModel.onViewConnectedStoresButtonClick()
        }
    }

    private fun updateUserInfoView(userInfo: SitePickerViewModel.UserInfo) {
        binding.loginUserInfo.displayName = userInfo.displayName
        binding.loginUserInfo.userName = userInfo.username
        binding.loginUserInfo.avatarUrl(userInfo.userAvatarUrl)
    }

    private fun updateSkeletonView(visible: Boolean) {
        when (visible) {
            true -> {
                skeletonView.show(binding.sitePickerRoot, R.layout.skeleton_site_picker, delayed = true)
            }

            false -> skeletonView.hide()
        }
    }

    private fun showProgressDialog(show: Boolean) {
        progressDialog?.dismiss()
        if (show) {
            progressDialog = CustomProgressDialog.show(
                getString(R.string.login_verifying_site),
                getString(R.string.product_update_dialog_message)
            ).also {
                it.show(parentFragmentManager, CustomProgressDialog.TAG)
            }
            progressDialog?.isCancelable = false
        }
    }

    private fun showWooUpgradeDialog() {
        WooUpgradeRequiredDialog.show().also {
            it.show(parentFragmentManager, WooUpgradeRequiredDialog.TAG)
        }
    }

    private fun navigateToNewToWooScreen() {
        ChromeCustomTabUtils.launchUrl(requireContext(), AppUrls.NEW_TO_WOO_DOC)
    }

    private fun navigateToAddStoreScreen() {
        findNavController()
            .navigateSafely(SitePickerFragmentDirections.actionSitePickerFragmentToSitePickerSiteDiscoveryFragment())
    }

    private fun navigateToNeedHelpFindingEmailScreen() {
        LoginEmailHelpDialogFragment.newInstance(this).also {
            it.show(parentFragmentManager, LoginEmailHelpDialogFragment.TAG)
        }
    }

    private fun navigateToWPComWebView(event: NavigateToWPComWebView) {
        findNavController().navigate(
            NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                urlToLoad = event.url,
                urlsToTriggerExit = arrayOf(event.validationUrl),
                title = event.title
            )
        )
    }

    private fun navigateToAccountMismatchScreen(event: NavigateToAccountMismatchScreen) {
        findNavController().navigateSafely(
            SitePickerFragmentDirections.actionSitePickerFragmentToAccountMismatchErrorFragment(
                siteUrl = event.siteUrl,
                primaryButton = event.primaryButton,
                errorType = AccountMismatchErrorType.WPCOM_ACCOUNT_MISMATCH
            )
        )
    }

    private fun onLogout() {
        activity?.setResult(Activity.RESULT_CANCELED)
        val intent = Intent(context, LoginActivity::class.java)
        LoginMode.WOO_LOGIN_MODE.putInto(intent)
        startActivity(intent)
        activity?.finish()
    }

    override fun onEmailNeedMoreHelpClicked() {
        startActivity(HelpActivity.createIntent(requireContext(), HelpOrigin.LOGIN_CONNECTED_EMAIL_HELP, null))
    }
}
