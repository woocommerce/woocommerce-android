package com.woocommerce.android.ui.compose.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.woocommerce.android.R

@Composable
fun SelectionCheck(
    isSelected: Boolean,
    onSelectionChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val selectionDrawable = if (isSelected) {
        R.drawable.ic_rounded_chcekbox_checked
    } else {
        R.drawable.ic_rounded_chcekbox_unchecked
    }

    val colorFilter = if (isEnabled) null else ColorFilter.tint(Color.Gray)

    val description = stringResource(id = R.string.card_selection_control)
    val state = if (!isEnabled) stringResource(id = R.string.disabled) else ""

    val controlModifier = if (isEnabled && onSelectionChange != null) {
        modifier.clickable { onSelectionChange(!isSelected) }
    } else {
        modifier
    }

    Box(
        modifier = controlModifier
            .semantics {
                contentDescription = description
                stateDescription = state
            },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = selectionDrawable,
            modifier = modifier.wrapContentSize(),
            label = "itemSelection"
        ) { icon ->
            Image(
                painter = painterResource(id = icon),
                colorFilter = colorFilter,
                contentDescription = null
            )
        }
    }
}
