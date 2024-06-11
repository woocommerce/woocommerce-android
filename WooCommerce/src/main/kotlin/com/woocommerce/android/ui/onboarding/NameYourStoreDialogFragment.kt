package com.woocommerce.android.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.onboarding.NameYourStoreViewModel.NameYourStoreDialogState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NameYourStoreDialogFragment : DialogFragment() {
    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: NameYourStoreViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    viewModel.viewState.observeAsState().value?.let { state ->
                        val focusRequester = remember { FocusRequester() }
                        Column(modifier = Modifier.clip(RoundedCornerShape(35.dp))) {
                            if (state.isLoading) {
                                LoadingDialog()
                            } else {
                                NameYourStoreDialog(state, focusRequester, state.isError)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
            }
        }
    }

    @Composable
    private fun NameYourStoreDialog(
        state: NameYourStoreDialogState,
        focusRequester: FocusRequester,
        isError: Boolean
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.store_onboarding_name_your_store_dialog_title),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
            )
            WCOutlinedTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(dimensionResource(id = R.dimen.major_100)),
                value = state.enteredSiteTitle,
                onValueChange = { viewModel.onSiteTitleInputChanged(it) },
                label = stringResource(id = R.string.store_onboarding_name_your_store_dialog_label),
                singleLine = true
            )

            Text(
                text = stringResource(id = R.string.store_onboarding_name_your_store_dialog_failure),
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                    .alpha(if (isError) 1f else 0f)
            )

            Row(
                modifier = Modifier
                    .padding(
                        start = dimensionResource(id = R.dimen.major_100),
                        end = dimensionResource(id = R.dimen.major_100),
                        bottom = dimensionResource(id = R.dimen.minor_100)
                    )
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WCTextButton(
                    text = stringResource(id = R.string.cancel),
                    onClick = viewModel::onNameYourStoreDismissed
                )
                WCTextButton(
                    text = stringResource(id = R.string.dialog_ok),
                    onClick = {
                        viewModel.saveSiteTitle(state.enteredSiteTitle)
                    }
                )
            }
        }
    }

    @Composable
    private fun LoadingDialog() {
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_150))
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.settings_name_your_store_dialog_loading),
                style = MaterialTheme.typography.h6
            )
            Spacer(modifier = Modifier.size(size = dimensionResource(id = R.dimen.major_150)))
            CircularProgressIndicator(modifier = Modifier.size(size = dimensionResource(id = R.dimen.major_150)))
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
