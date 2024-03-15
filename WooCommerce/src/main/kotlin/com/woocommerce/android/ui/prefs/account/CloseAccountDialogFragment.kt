package com.woocommerce.android.ui.prefs.account

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.prefs.account.CloseAccountViewModel.CloseAccountState
import com.woocommerce.android.ui.prefs.account.CloseAccountViewModel.ContactSupport
import com.woocommerce.android.ui.prefs.account.CloseAccountViewModel.OnAccountClosed
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginMode

@AndroidEntryPoint
class CloseAccountDialogFragment : DialogFragment() {

    private val viewModel: CloseAccountViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Specify transition animations
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
                                AccountClosingDialog(state, focusRequester)
                            }
                        }
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Event.Exit -> findNavController().popBackStack()
                is ContactSupport -> {
                    SupportRequestFormActivity.createIntent(
                        context = requireContext(),
                        origin = event.origin,
                        extraTags = ArrayList()
                    ).let { activity?.startActivity(it) }
                    findNavController().popBackStack()
                }

                is OnAccountClosed -> onAccountClosed()
            }
        }
    }

    private fun onAccountClosed() {
        findNavController().popBackStack()
        val intent = Intent(context, LoginActivity::class.java)
        LoginMode.WOO_LOGIN_MODE.putInto(intent)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        activity?.finish()
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
                text = stringResource(id = R.string.settings_close_account_dialog_loading_title),
                style = MaterialTheme.typography.h6
            )
            Spacer(modifier = Modifier.size(size = dimensionResource(id = R.dimen.major_150)))
            CircularProgressIndicator(modifier = Modifier.size(size = dimensionResource(id = R.dimen.major_150)))
        }
    }

    @Composable
    private fun AccountClosingDialog(
        state: CloseAccountState,
        focusRequester: FocusRequester
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_125)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_75)),
        ) {
            Text(
                text = stringResource(id = state.title),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = state.description),
                style = MaterialTheme.typography.subtitle1,
            )
            if (!state.isAccountDeletionError) {
                Text(
                    text = state.currentUserName,
                    style = MaterialTheme.typography.subtitle1,
                )
                WCOutlinedTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .padding(top = dimensionResource(id = R.dimen.minor_100)),
                    value = state.enteredUserName,
                    onValueChange = { viewModel.onUserNameInputChanged(it) },
                    label = stringResource(id = R.string.username),
                    singleLine = true,
                )
            }
        }
        Row(
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_100),
                end = dimensionResource(id = R.dimen.major_100),
                bottom = dimensionResource(id = R.dimen.minor_100),
            ),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WCTextButton(
                onClick = viewModel::onCloseAccountDismissed
            ) {
                Text(
                    text = stringResource(id = R.string.cancel),
                    style = MaterialTheme.typography.subtitle1
                )
            }
            WCTextButton(
                onClick = if (state.isAccountDeletionError) {
                    { viewModel.onContactSupportClicked() }
                } else {
                    { viewModel.onConfirmCloseAccount() }
                },
                enabled = state.enteredUserName == state.currentUserName
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(id = state.mainButtonText),
                        style = MaterialTheme.typography.subtitle1,
                        textAlign = TextAlign.Center,
                        color = colorResource(
                            when {
                                state.isAccountDeletionError -> R.color.color_primary
                                state.enteredUserName == state.currentUserName -> R.color.color_error
                                else -> R.color.color_on_surface_disabled
                            }
                        )
                    )
                }
            }
        }
    }
}
