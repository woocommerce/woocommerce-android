package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosFloatingKeyboardHint(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    actionText: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    WooPosFloatingKeyboardHintContent(
        modifier = modifier,
        title = title,
        message = message,
        actionText = actionText,
        onDismiss = onDismiss,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun WooPosFloatingKeyboardHintContent(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    actionText: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    WooPosCard(
        modifier = modifier
            .semantics { contentDescription = title },
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val icon = createRef()
            val titleText = createRef()
            val messageText = createRef()
            val closeButton = createRef()

            Box(
                modifier = Modifier
                    .size(112.dp)
                    .constrainAs(icon) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    modifier = Modifier
                        .size(54.dp),
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            WooPosText(
                text = title,
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(titleText) {
                    start.linkTo(icon.end)
                    end.linkTo(parent.end, margin = WooPosSpacing.Medium.value)
                    bottom.linkTo(messageText.top)
                    top.linkTo(parent.top, margin = WooPosSpacing.Medium.value)
                    width = Dimension.fillToConstraints
                }
            )

            val linkAnnotation = LinkAnnotation.Clickable(
                tag = "settings"
            ) {
                onOpenSettings()
            }

            val annotatedMessage = buildAnnotatedString {
                append(message)
                append(" ")
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    withLink(linkAnnotation) {
                        append(actionText)
                    }
                }
            }

            WooPosText(
                text = annotatedMessage,
                style = WooPosTypography.BodySmall,
                modifier = Modifier
                    .constrainAs(messageText) {
                        start.linkTo(titleText.start)
                        end.linkTo(closeButton.start, margin = WooPosSpacing.Medium.value)
                        bottom.linkTo(parent.bottom, margin = WooPosSpacing.Medium.value)
                        top.linkTo(titleText.bottom, margin = WooPosSpacing.Small.value)
                        width = Dimension.fillToConstraints
                    }
                    .clickable { onOpenSettings() }
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .constrainAs(closeButton) {
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                    }
            ) {
                Icon(
                    modifier = Modifier
                        .size(32.dp),
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(
                        id = R.string.woopos_exit_dialog_confirmation_close_content_description
                    ),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@WooPosPreview
@Composable
fun WooPosFloatingKeyboardHintPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(WooPosSpacing.Large.value)
        ) {
            WooPosFloatingKeyboardHint(
                title = stringResource(R.string.woopos_keyboard_hint_title),
                message = stringResource(R.string.woopos_keyboard_hint_message),
                actionText = stringResource(R.string.woopos_keyboard_hint_action),
                onDismiss = {},
                onOpenSettings = {}
            )
        }
    }
}
