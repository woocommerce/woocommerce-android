package com.woocommerce.android.ui.prefs.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCSearchField
import com.woocommerce.android.util.FeatureFlag

@Composable
fun DevFeatureFlagsScreen(onBackClick: () -> Unit) {
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
                    androidx.compose.material.Text(text = stringResource(R.string.dev_feature_flags))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_back_24dp),
                            contentDescription = stringResource(R.string.back)
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
                items(filteredFlags) { flag ->
                    FeatureFlagItem(flag)
                }
            }
        }
    }
}

@Composable
private fun FeatureFlagItem(flag: FeatureFlag) {
    var isEnabled by remember {
        mutableStateOf(flag.isEnabled())
    }
    val defaultValue = remember { flag.getDefaultValue() }
    val defaultText = if (defaultValue) "Default: enabled" else "Default: disabled"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                isEnabled = !isEnabled
                AppPrefs.setFeatureFlagOverride(flag, isEnabled)
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = flag.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = defaultText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { checked ->
                    isEnabled = checked
                    AppPrefs.setFeatureFlagOverride(flag, checked)
                }
            )
        }

        HorizontalDivider()
    }
}
