package com.woocommerce.android.ui.dashboard

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.clickableAnnotatedStringRes
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.LaunchUrlInChromeTab
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScheduledImportInfoBottomSheetFragment : WCBottomSheetDialogFragment() {
    private val viewModel: ScheduledImportInfoViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooTheme {
                    val state by viewModel.viewState.observeAsState()
                    state?.let {
                        ScheduledImportInfoContent(
                            state = it,
                            onOptionSelected = viewModel::onOptionSelected,
                            onLearnMoreClick = viewModel::onLearnMoreClicked
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ScheduledImportInfoViewModel.SettingUpdated -> dismiss()

                is LaunchUrlInChromeTab ->
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)

                else -> event.isHandled = false
            }
        }
    }
}

@Composable
private fun ScheduledImportInfoContent(
    state: ScheduledImportInfoViewModel.ViewState,
    onOptionSelected: (Boolean) -> Unit,
    onLearnMoreClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = R.dimen.minor_100),
            topEnd = dimensionResource(id = R.dimen.minor_100)
        )
    ) {
        // The horizontal padding is applied per child (not on the Column) so the tappable option
        // rows can draw their ripple across the full width of the sheet.
        val contentPadding = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
            BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))

            Text(
                text = stringResource(id = R.string.dashboard_scheduled_import_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.color_on_surface_high),
                modifier = contentPadding
            )

            Text(
                text = clickableAnnotatedStringRes(
                    stringResId = R.string.dashboard_scheduled_import_sheet_description,
                    onUrlClick = { onLearnMoreClick() }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(id = R.color.color_on_surface_medium),
                modifier = contentPadding
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_50)))

            AnalyticsUpdateOption(
                title = stringResource(id = R.string.dashboard_scheduled_import_option_scheduled_title),
                description = stringResource(id = R.string.dashboard_scheduled_import_option_scheduled_description),
                isSelected = state.isEnabled,
                isLoading = state.isUpdating && state.isEnabled,
                enabled = !state.isUpdating,
                onClick = { onOptionSelected(true) }
            )

            AnalyticsUpdateOption(
                title = stringResource(id = R.string.dashboard_scheduled_import_option_immediately_title),
                description = stringResource(id = R.string.dashboard_scheduled_import_option_immediately_description),
                isSelected = !state.isEnabled,
                isLoading = state.isUpdating && !state.isEnabled,
                enabled = !state.isUpdating,
                onClick = { onOptionSelected(false) }
            )

            if (state.hasError) {
                Text(
                    text = stringResource(id = R.string.dashboard_scheduled_import_update_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = contentPadding
                )
            }

            Text(
                text = stringResource(id = R.string.dashboard_scheduled_import_store_wide_note),
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(id = R.color.color_on_surface_medium),
                modifier = contentPadding
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        }
    }
}

@Composable
private fun AnalyticsUpdateOption(
    title: String,
    description: String,
    isSelected: Boolean,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_100)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.color_on_surface_high)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
        }
        // Fixed-size trailing slot so the text column keeps a constant width whether or not
        // the checkmark/progress is shown.
        Box(
            modifier = Modifier
                .padding(start = dimensionResource(id = R.dimen.major_100))
                .size(dimensionResource(id = R.dimen.major_150)),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.size(dimensionResource(id = R.dimen.major_125)),
                    strokeWidth = 2.dp,
                    color = colorResource(id = R.color.color_primary)
                )

                isSelected -> Icon(
                    painter = painterResource(id = R.drawable.ic_check_24dp),
                    contentDescription = stringResource(id = R.string.dashboard_scheduled_import_option_selected),
                    tint = colorResource(id = R.color.color_primary)
                )
            }
        }
    }
}

@Composable
@Preview(name = "scheduled selected - light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "scheduled selected - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "large screen", device = Devices.NEXUS_10)
private fun ScheduledImportInfoContentScheduledPreview() {
    WooThemeWithBackground {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            ScheduledImportInfoContent(
                state = ScheduledImportInfoViewModel.ViewState(
                    isEnabled = true,
                    isUpdating = false,
                    hasError = false
                ),
                onOptionSelected = {},
                onLearnMoreClick = {}
            )
        }
    }
}

@Composable
@Preview(name = "immediately selected - light", uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun ScheduledImportInfoContentImmediatelyPreview() {
    WooThemeWithBackground {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            ScheduledImportInfoContent(
                state = ScheduledImportInfoViewModel.ViewState(
                    isEnabled = false,
                    isUpdating = false,
                    hasError = true
                ),
                onOptionSelected = {},
                onLearnMoreClick = {}
            )
        }
    }
}

@Composable
@Preview(name = "updating - light", uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun ScheduledImportInfoContentUpdatingPreview() {
    WooThemeWithBackground {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            ScheduledImportInfoContent(
                state = ScheduledImportInfoViewModel.ViewState(
                    isEnabled = true,
                    isUpdating = true,
                    hasError = false
                ),
                onOptionSelected = {},
                onLearnMoreClick = {}
            )
        }
    }
}
