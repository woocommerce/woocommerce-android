package com.woocommerce.android.ui.login.storecreation.iapeligibility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.iapeligibility.IapEligibilityViewModel.IapEligibilityEvent.NavigateToNativeStoreCreation
import com.woocommerce.android.ui.login.storecreation.iapeligibility.IapEligibilityViewModel.IapEligibilityEvent.NavigateToWebStoreCreation
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CheckIapEligibilityFragment : BaseFragment() {
    private val viewModel: IapEligibilityViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dimensionResource(id = R.dimen.major_100)),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text(
                            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_200)),
                            text = stringResource(id = R.string.store_creation_iap_eligibility_loading_title),
                            style = MaterialTheme.typography.h6,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)),
                            text = stringResource(id = R.string.store_creation_iap_eligibility_loading_subtitle),
                            style = MaterialTheme.typography.subtitle1,
                            color = colorResource(id = R.color.color_on_surface_medium)
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigateToWebStoreCreation -> navigateToStoreCreationWeb()
                is NavigateToNativeStoreCreation -> navigateToStoreCreationNative()
            }
        }
    }

    private fun navigateToStoreCreationWeb() {
        findNavController()
            .navigateSafely(
                CheckIapEligibilityFragmentDirections.actionCheckIapEligibilityFragmentToWebViewStoreCreationFragment()
            )
    }

    private fun navigateToStoreCreationNative() {
        findNavController()
            .navigateSafely(
                CheckIapEligibilityFragmentDirections.actionCheckIapEligibilityFragmentToStoreNamePickerFragment()
            )
    }
}
