package com.woocommerce.android.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.clickableAnnotatedStringRes
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooModalBottomSheet
import com.woocommerce.android.ui.compose.designsystem.component.rememberWooModalBottomSheetDismisser
import com.woocommerce.android.ui.compose.designsystem.component.rememberWooModalBottomSheetState
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground

@Composable
internal fun ScheduledImportInfoBottomSheet(viewModel: ScheduledImportInfoViewModel) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    ScheduledImportInfoBottomSheet(
        state = state,
        onDismissRequest = viewModel::onDismissed,
        onOptionSelected = viewModel::onOptionSelected,
        onLearnMoreClick = viewModel::onLearnMoreClicked,
    )
}

@Composable
private fun ScheduledImportInfoBottomSheet(
    state: ScheduledImportInfoViewModel.ViewState,
    onDismissRequest: () -> Unit,
    onOptionSelected: (Boolean) -> Unit,
    onLearnMoreClick: () -> Unit,
) {
    if (!state.isVisible) return

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
        ScheduledImportInfoContent(
            state = state,
            onOptionSelected = onOptionSelected,
            onLearnMoreClick = onLearnMoreClick,
        )
    }
}

@Composable
private fun ScheduledImportInfoContent(
    state: ScheduledImportInfoViewModel.ViewState,
    onOptionSelected: (Boolean) -> Unit,
    onLearnMoreClick: () -> Unit,
) {
    val contentPadding = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
    ) {
        Text(
            text = stringResource(id = R.string.dashboard_scheduled_import_sheet_title),
            style = WooTheme.text.titleLarge.strong,
            color = WooTheme.colors.surface.onDefault,
            modifier = contentPadding,
        )

        Text(
            text = clickableAnnotatedStringRes(
                stringResId = R.string.dashboard_scheduled_import_sheet_description,
                onUrlClick = { onLearnMoreClick() },
            ),
            style = WooTheme.text.bodyMedium.regular,
            color = WooTheme.colors.surface.onVariant,
            modifier = contentPadding,
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_50)))

        AnalyticsUpdateOption(
            title = stringResource(id = R.string.dashboard_scheduled_import_option_scheduled_title),
            description = stringResource(id = R.string.dashboard_scheduled_import_option_scheduled_description),
            isSelected = state.isEnabled,
            isLoading = state.isUpdating && state.isEnabled,
            enabled = !state.isUpdating && !state.isDismissRequested,
            onClick = { onOptionSelected(true) },
        )

        AnalyticsUpdateOption(
            title = stringResource(id = R.string.dashboard_scheduled_import_option_immediately_title),
            description = stringResource(id = R.string.dashboard_scheduled_import_option_immediately_description),
            isSelected = !state.isEnabled,
            isLoading = state.isUpdating && !state.isEnabled,
            enabled = !state.isUpdating && !state.isDismissRequested,
            onClick = { onOptionSelected(false) },
        )

        if (state.hasError) {
            Text(
                text = stringResource(id = R.string.dashboard_scheduled_import_update_error),
                style = WooTheme.text.bodySmall.regular,
                color = WooTheme.colors.error,
                modifier = contentPadding,
            )
        }

        Text(
            text = stringResource(id = R.string.dashboard_scheduled_import_store_wide_note),
            style = WooTheme.text.bodySmall.regular,
            color = WooTheme.colors.surface.onVariant,
            modifier = contentPadding,
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    }
}

@Composable
private fun AnalyticsUpdateOption(
    title: String,
    description: String,
    isSelected: Boolean,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_100),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50)),
        ) {
            Text(
                text = title,
                style = WooTheme.text.titleMedium.strong,
                color = WooTheme.colors.surface.onDefault,
            )
            Text(
                text = description,
                style = WooTheme.text.bodyMedium.regular,
                color = WooTheme.colors.surface.onVariant,
            )
        }
        Box(
            modifier = Modifier
                .padding(start = dimensionResource(id = R.dimen.major_100))
                .size(dimensionResource(id = R.dimen.major_150)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.size(dimensionResource(id = R.dimen.major_125)),
                    strokeWidth = 2.dp,
                    color = WooTheme.colors.primary,
                )

                isSelected -> Icon(
                    painter = painterResource(id = R.drawable.ic_check_24dp),
                    contentDescription = stringResource(id = R.string.dashboard_scheduled_import_option_selected),
                    tint = WooTheme.colors.primary,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ScheduledImportInfoBottomSheetScheduledPreview() {
    ScheduledImportInfoBottomSheetPreview(
        state = ScheduledImportInfoViewModel.ViewState(
            isVisible = true,
            isEnabled = true,
            isUpdating = false,
            hasError = false,
        )
    )
}

@PreviewLightDark
@Composable
private fun ScheduledImportInfoBottomSheetImmediatelyPreview() {
    ScheduledImportInfoBottomSheetPreview(
        state = ScheduledImportInfoViewModel.ViewState(
            isVisible = true,
            isEnabled = false,
            isUpdating = false,
            hasError = true,
        )
    )
}

@PreviewLightDark
@Composable
private fun ScheduledImportInfoBottomSheetUpdatingPreview() {
    ScheduledImportInfoBottomSheetPreview(
        state = ScheduledImportInfoViewModel.ViewState(
            isVisible = true,
            isEnabled = true,
            isUpdating = true,
            hasError = false,
        )
    )
}

@Preview(name = "Scheduled selected - large screen", device = Devices.NEXUS_10)
@Composable
private fun ScheduledImportInfoBottomSheetLargeScreenPreview() {
    ScheduledImportInfoBottomSheetPreview(
        state = ScheduledImportInfoViewModel.ViewState(
            isVisible = true,
            isEnabled = true,
            isUpdating = false,
            hasError = false,
        )
    )
}

@Composable
private fun ScheduledImportInfoBottomSheetPreview(state: ScheduledImportInfoViewModel.ViewState) {
    WooDesignSystemThemeWithBackground {
        ScheduledImportInfoBottomSheet(
            state = state,
            onDismissRequest = {},
            onOptionSelected = {},
            onLearnMoreClick = {},
        )
    }
}
