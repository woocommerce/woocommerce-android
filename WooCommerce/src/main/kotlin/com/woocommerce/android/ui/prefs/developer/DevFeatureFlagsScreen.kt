package com.woocommerce.android.ui.prefs.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCSearchField
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository.FeatureFlagState

@Composable
fun DevFeatureFlagsScreen(
    onBackClick: () -> Unit,
    onRestartClick: () -> Unit,
    viewModel: DevFeatureFlagsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DevFeatureFlagsScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRestartClick = onRestartClick,
        onOverrideChange = viewModel::setOverride
    )
}

@Composable
private fun DevFeatureFlagsScreenContent(
    uiState: DevFeatureFlagsViewModel.UiState,
    onBackClick: () -> Unit,
    onRestartClick: () -> Unit,
    onOverrideChange: (FeatureFlag, OverrideState) -> Unit
) {
    val flagStates = uiState.flagStates
    val allFeatureFlags = remember { FeatureFlag.entries.toList() }
    var searchQuery by remember { mutableStateOf("") }
    val filteredFlags by remember(searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                allFeatureFlags
            } else {
                allFeatureFlags.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.dev_feature_flags))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_back_24dp),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onRestartClick,
                        enabled = uiState.hasChanges,
                    ) {
                        Text(
                            text = stringResource(R.string.restart).uppercase(),
                            color = if (uiState.hasChanges) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            }
                        )
                    }
                },
                backgroundColor = MaterialTheme.colorScheme.surface,
                elevation = 4.dp
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            WCSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                hint = stringResource(R.string.search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(id = R.dimen.major_100))
            )

            LazyColumn {
                items(filteredFlags, key = { it.name }) { flag ->
                    flagStates[flag]?.let { state ->
                        FeatureFlagItem(
                            state = state,
                            onOverrideChange = { overrideState ->
                                onOverrideChange(flag, overrideState)
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class OverrideState {
    DEFAULT,
    DISABLED,
    ENABLED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureFlagItem(
    state: FeatureFlagState,
    onOverrideChange: (OverrideState) -> Unit
) {
    val flag = state.flag
    val currentOverrideState = when (state.overrideValue) {
        null -> OverrideState.DEFAULT
        true -> OverrideState.ENABLED
        false -> OverrideState.DISABLED
    }

    val segmentedButtonColors = SegmentedButtonDefaults.colors(
        activeContainerColor = colorResource(id = R.color.color_primary),
        activeContentColor = colorResource(id = R.color.woo_white),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Content with padding
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = flag.remoteFlagKey,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Default button
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    onClick = { onOverrideChange(OverrideState.DEFAULT) },
                    selected = currentOverrideState == OverrideState.DEFAULT,
                    colors = segmentedButtonColors,
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(getSourceStateText(state))
                }

                // Disabled button
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    onClick = { onOverrideChange(OverrideState.DISABLED) },
                    selected = currentOverrideState == OverrideState.DISABLED,
                    colors = segmentedButtonColors,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disabled")
                }

                // Enabled button
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    onClick = { onOverrideChange(OverrideState.ENABLED) },
                    selected = currentOverrideState == OverrideState.ENABLED,
                    colors = segmentedButtonColors,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enabled")
                }
            }
        }

        HorizontalDivider()
    }
}

private fun getSourceStateText(state: FeatureFlagState): String {
    fun getStateText(enabled: Boolean) = if (enabled) "Enabled" else "Disabled"

    val remoteValue = state.remoteValue
    return if (!state.localValue) {
        "Local: ${getStateText(state.localValue)}"
    } else if (remoteValue != null) {
        "Remote: ${getStateText(remoteValue)}"
    } else {
        "Local: ${getStateText(state.localValue)}"
    }
}
