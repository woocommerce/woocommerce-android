package com.woocommerce.android.ui.login.storesuggestion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledButton
import com.woocommerce.android.ui.compose.designsystem.component.WooIconContainer
import com.woocommerce.android.ui.compose.designsystem.component.WooModalBottomSheet
import com.woocommerce.android.ui.compose.designsystem.component.rememberWooModalBottomSheetDismisser
import com.woocommerce.android.ui.compose.designsystem.component.rememberWooModalBottomSheetState
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.Globe
import com.woocommerce.android.ui.compose.designsystem.icons.Store
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
fun LoginStoreSuggestionBottomSheet(viewModel: LoginStoreSuggestionViewModel) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    state?.let {
        LoginStoreSuggestionBottomSheet(
            state = it,
            onLogInToStoreClick = viewModel::onLogInToStoreClicked,
            onUseDifferentStoreClick = viewModel::onUseDifferentStoreClicked,
            onDismissRequest = viewModel::onDismissed,
        )
    }
}

@Composable
private fun LoginStoreSuggestionBottomSheet(
    state: LoginStoreSuggestionViewModel.ViewState,
    onLogInToStoreClick: () -> Unit,
    onUseDifferentStoreClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberWooModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetDismisser = rememberWooModalBottomSheetDismisser(
        state = sheetState,
        onDismissed = onDismissRequest,
    )

    LaunchedEffect(state.isDismissRequested) {
        if (state.isDismissRequested) {
            sheetDismisser.dismiss()
        }
    }

    WooModalBottomSheet(
        state = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        LoginStoreSuggestionContent(
            displayAddress = state.displayAddress,
            onLogInToStoreClick = onLogInToStoreClick,
            onUseDifferentStoreClick = onUseDifferentStoreClick,
        )
    }
}

@Composable
private fun LoginStoreSuggestionContent(
    displayAddress: String,
    onLogInToStoreClick: () -> Unit,
    onUseDifferentStoreClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WooTheme.spacing.space5)
            .padding(bottom = WooTheme.spacing.space5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        WooIconContainer(imageVector = WooIcons.Regular.Store)

        Text(
            text = stringResource(R.string.login_store_suggestion_title),
            style = WooTheme.text.titleLarge.strong,
            color = WooTheme.colors.surface.onDefault,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )

        Text(
            text = stringResource(R.string.login_store_suggestion_description),
            style = WooTheme.text.bodyMedium.regular,
            color = WooTheme.colors.surface.onVariant,
            textAlign = TextAlign.Center,
        )

        StoreAddress(
            displayAddress = displayAddress,
            modifier = Modifier.padding(vertical = WooTheme.spacing.space2),
        )

        WooFilledButton(
            text = stringResource(R.string.login_store_suggestion_log_in),
            onClick = onLogInToStoreClick,
            modifier = Modifier.fillMaxWidth(),
        )

        TextButton(
            onClick = onUseDifferentStoreClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.login_store_suggestion_use_different_store),
                style = WooTheme.text.labelLarge.emphasized,
                color = WooTheme.colors.primary,
            )
        }
    }
}

@Composable
private fun StoreAddress(
    displayAddress: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WooTheme.radius.medium),
        border = BorderStroke(WooTheme.stroke.extraThin, WooTheme.colors.outlineVariant),
        color = WooTheme.colors.surface.default,
    ) {
        Row(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = WooTheme.spacing.space4, vertical = WooTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2, Alignment.CenterHorizontally),
        ) {
            Icon(
                imageVector = WooIcons.Regular.Globe,
                contentDescription = null,
                tint = WooTheme.colors.surface.onVariant,
                modifier = Modifier.size(WooTheme.iconSize.size16),
            )
            Text(
                text = displayAddress,
                style = WooTheme.text.bodyMedium.emphasized,
                color = WooTheme.colors.surface.onDefault,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LoginStoreSuggestionContentPreview() {
    WooDesignSystemThemeWithBackground {
        LoginStoreSuggestionContent(
            displayAddress = "impossibly-regular-ibis.jurassic.ninja",
            onLogInToStoreClick = {},
            onUseDifferentStoreClick = {},
        )
    }
}
