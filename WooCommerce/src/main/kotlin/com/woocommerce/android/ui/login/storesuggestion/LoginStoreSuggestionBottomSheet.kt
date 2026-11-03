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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledButton
import com.woocommerce.android.ui.compose.designsystem.component.WooIconContainer
import com.woocommerce.android.ui.compose.designsystem.component.WooModalBottomSheet
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.component.rememberWooModalBottomSheetDismisser
import com.woocommerce.android.ui.compose.designsystem.component.rememberWooModalBottomSheetState
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.Globe
import com.woocommerce.android.ui.compose.designsystem.icons.Store
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import kotlinx.coroutines.delay

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
            host = state.host,
            registrableDomain = state.registrableDomain,
            onLogInToStoreClick = onLogInToStoreClick,
            onUseDifferentStoreClick = onUseDifferentStoreClick,
        )
    }
}

@Composable
private fun LoginStoreSuggestionContent(
    host: String,
    registrableDomain: String?,
    onLogInToStoreClick: () -> Unit,
    onUseDifferentStoreClick: () -> Unit,
) {
    var areButtonsEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(BUTTONS_ENABLE_DELAY_MILLIS)
        areButtonsEnabled = true
    }

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
            host = host,
            registrableDomain = registrableDomain,
            modifier = Modifier.padding(vertical = WooTheme.spacing.space2),
        )

        WooFilledButton(
            text = stringResource(R.string.login_store_suggestion_log_in),
            onClick = onLogInToStoreClick,
            enabled = areButtonsEnabled,
            modifier = Modifier.fillMaxWidth(),
        )

        WooOutlinedButton(
            text = stringResource(R.string.login_store_suggestion_use_different_store),
            onClick = onUseDifferentStoreClick,
            enabled = areButtonsEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StoreAddress(
    host: String,
    registrableDomain: String?,
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
                text = hostWithEmphasizedDomain(host, registrableDomain),
                style = WooTheme.text.bodyLarge.regular,
                color = WooTheme.colors.surface.onDefault,
                textAlign = TextAlign.Center,
                softWrap = true,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

private fun hostWithEmphasizedDomain(host: String, registrableDomain: String?): AnnotatedString =
    buildAnnotatedString {
        val (prefix, emphasized) = splitHostForEmphasis(host, registrableDomain)
        append(prefix)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(emphasized) }
    }

internal fun splitHostForEmphasis(host: String, registrableDomain: String?): Pair<String, String> {
    val domain = registrableDomain?.takeIf { it.isNotEmpty() && (host == it || host.endsWith(".$it")) }
        ?: return "" to host
    return host.removeSuffix(domain) to domain
}

private const val BUTTONS_ENABLE_DELAY_MILLIS = 500L

@PreviewLightDark
@Composable
private fun LoginStoreSuggestionContentPreview() {
    WooDesignSystemThemeWithBackground {
        LoginStoreSuggestionContent(
            host = "impossibly-regular-ibis.jurassic.ninja",
            registrableDomain = "jurassic.ninja",
            onLogInToStoreClick = {},
            onUseDifferentStoreClick = {},
        )
    }
}
