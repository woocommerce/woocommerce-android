package com.woocommerce.android.ui.login.storecreation.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ThemeActivationFragmentDialog : DialogFragment() {
    companion object {
        const val THEME_ACTIVATION_FINISHED = "theme_activation_finished"
    }

    private val viewModel: ThemeActivationViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        isCancelable = false

        return composeView {
            ThemeActivationScreen(viewModel = viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Event.Exit -> navigateBackWithNotice(THEME_ACTIVATION_FINISHED)
            }
        }
    }
}

@Composable
private fun ThemeActivationScreen(viewModel: ThemeActivationViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        when (state) {
            is ThemeActivationViewModel.ViewState.LoadingState -> ThemeActivationLoading()
            is ThemeActivationViewModel.ViewState.ErrorState -> ThemeActivationError(
                onRetry = state.onRetry,
                onDismiss = state.onDismiss
            )
        }
    }
}

@Composable
private fun ThemeActivationLoading() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = stringResource(id = R.string.store_creation_theme_activation_loading),
            textAlign = TextAlign.Center
        )

        CircularProgressIndicator()
    }
}

@Composable
private fun ThemeActivationError(
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = stringResource(id = R.string.store_creation_theme_activation_error),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))

        WCColoredButton(onClick = onRetry, text = stringResource(id = R.string.retry))
        WCTextButton(onClick = onDismiss, text = stringResource(id = R.string.dismiss))
    }
}

@Composable
@Preview(showBackground = true)
private fun ThemeActivationLoadingPreview() {
    WooThemeWithBackground {
        ThemeActivationLoading()
    }
}

@Composable
@Preview(showBackground = true)
private fun ThemeActivationErrorPreview() {
    WooThemeWithBackground {
        ThemeActivationError(onRetry = {}, onDismiss = {})
    }
}
