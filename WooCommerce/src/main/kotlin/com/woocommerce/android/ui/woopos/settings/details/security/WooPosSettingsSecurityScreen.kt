package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosEmptyScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOverflowMenu
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOverflowMenuItem
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItemInfo

@Composable
fun WooPosSettingsSecurityScreen(
    state: WooPosSettingsSecurityState,
    onSetUpPIN: (WooPosLocalRole) -> Unit,
    onProtectionTurnedOff: () -> Unit,
    onChangePIN: (WooPosLocalRole) -> Unit,
    onDeleteCashierPIN: () -> Unit,
) {
    when (state) {
        WooPosSettingsSecurityState.Loading, WooPosSettingsSecurityState.Restricted -> Unit
        WooPosSettingsSecurityState.Off -> WooPosEmptyScreen(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(WooPosSpacing.Large.value),
            title = stringResource(R.string.woopos_security_setup),
            message = stringResource(R.string.woopos_security_description),
            image = {
                Image(painter = painterResource(R.drawable.woopos_access_control), contentDescription = null)
            },
            bottomActionLabel = stringResource(R.string.woopos_security_get_started),
            onActionClicked = { onSetUpPIN(WooPosLocalRole.ADMIN) },
        )
        WooPosSettingsSecurityState.Unavailable -> CenteredContent {
            WooPosText(
                text = stringResource(R.string.woopos_lock_unavailable),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        is WooPosSettingsSecurityState.Protected -> SecurityCards(
            state,
            onSetUpPIN,
            onProtectionTurnedOff,
            onChangePIN,
            onDeleteCashierPIN,
        )
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = WooPosSpacing.XXLarge.value, vertical = WooPosSpacing.Medium.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun SecurityCards(
    state: WooPosSettingsSecurityState.Protected,
    onSetUpPIN: (WooPosLocalRole) -> Unit,
    onProtectionTurnedOff: () -> Unit,
    onChangePIN: (WooPosLocalRole) -> Unit,
    onDeleteCashierPIN: () -> Unit,
) {
    val changeAdminLabel = stringResource(R.string.woopos_security_change_admin_pin)
    val setUpCashierLabel = stringResource(R.string.woopos_security_set_up_cashier_pin)
    val setUpAppPinningLabel = stringResource(R.string.woopos_security_set_up_app_pinning)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WooPosSpacing.Medium.value)
            .padding(bottom = WooPosSpacing.Medium.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        SecurityCard(stringResource(R.string.woopos_security_pin_protection)) {
            SecurityToggle(
                title = stringResource(R.string.woopos_security_enable_pin_protection),
                subtitle = stringResource(R.string.woopos_security_enable_pin_protection_detail),
                checked = true,
                onCheckedChange = { onProtectionTurnedOff() },
            )
            if (state.removalFailed) {
                WooPosText(
                    text = stringResource(R.string.woopos_security_save_failed),
                    style = WooPosTypography.BodySmall,
                    color = WooPosTheme.colors.onErrorLowest,
                    modifier = Modifier
                        .background(WooPosTheme.colors.errorLowest, MaterialTheme.shapes.small)
                        .padding(WooPosSpacing.Small.value)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            SecurityDivider()
            SecurityRow(
                title = stringResource(R.string.woopos_security_admin_pin),
                subtitle = stringResource(R.string.woopos_security_admin_pin_detail),
            ) {
                WooPosOutlinedButtonSmall(
                    modifier = Modifier.semantics { contentDescription = changeAdminLabel },
                    text = stringResource(R.string.woopos_security_change),
                    onClick = { onChangePIN(WooPosLocalRole.ADMIN) },
                )
            }
            SecurityDivider()
            SecurityRow(
                title = stringResource(R.string.woopos_security_cashier_pin),
                subtitle = stringResource(R.string.woopos_security_cashier_pin_detail),
            ) { stacked ->
                if (!state.hasCashierPIN) {
                    WooPosButtonSmall(
                        modifier = Modifier.semantics { contentDescription = setUpCashierLabel },
                        text = stringResource(R.string.woopos_security_set_up),
                        onClick = { onSetUpPIN(WooPosLocalRole.CASHIER) },
                    )
                } else {
                    CashierActions(stacked, { onChangePIN(WooPosLocalRole.CASHIER) }, onDeleteCashierPIN)
                }
            }
            SecurityDivider()
            TextButton(
                onClick = {},
                colors = securityTextButtonColors(),
                shape = RoundedCornerShape(WooPosCornerRadius.None.value),
                contentPadding = PaddingValues(WooPosSpacing.None.value),
            ) {
                WooPosText(
                    text = stringResource(R.string.woopos_security_learn_more),
                    style = WooPosTypography.BodyMedium,
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.Start,
                )
            }
        }
        LockSettingsCard()
        SecurityCard(stringResource(R.string.woopos_security_device_security)) {
            SecurityRow(
                title = stringResource(R.string.woopos_security_app_pinning),
                subtitle = stringResource(R.string.woopos_security_app_pinning_detail),
            ) {
                // TODO WOOMOB-4604: Add the App Pinning guide.
                WooPosButtonSmall(
                    modifier = Modifier.semantics { contentDescription = setUpAppPinningLabel },
                    text = stringResource(R.string.woopos_security_set_up),
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun SecurityCard(title: String, content: @Composable () -> Unit) {
    WooPosCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(WooPosSpacing.Medium.value)) {
            WooPosText(
                text = title,
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = WooPosSpacing.Small.value).semantics { heading() },
            )
            content()
        }
    }
}

@Composable
private fun SecurityDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Small.value))
}

@Composable
private fun SecurityToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = WooPosSpacing.Small.value),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        SecurityRowText(title, subtitle, Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
    }
}

@Composable
private fun SecurityRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable (stacked: Boolean) -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stacked = maxWidth < ROW_MIN_WIDTH * fontScale
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)) {
                SecurityRowText(title, subtitle)
                action(true)
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
            ) {
                SecurityRowText(title, subtitle, Modifier.weight(1f))
                action(false)
            }
        }
    }
}

@Composable
private fun SecurityRowText(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    if (subtitle != null) {
        WooPosSettingsDetailsMenuItemInfo(
            modifier = modifier,
            title = title,
            subtitle = subtitle,
            subtitleColor = WooPosTheme.colors.secondaryText,
        )
    } else {
        WooPosText(title, style = WooPosTypography.BodyMedium, modifier = modifier)
    }
}

@Composable
private fun CashierActions(stacked: Boolean, onChange: () -> Unit, onDelete: () -> Unit) {
    val change = stringResource(R.string.woopos_security_change)
    val delete = stringResource(R.string.woopos_security_delete)
    val changeLabel = stringResource(R.string.woopos_security_change_cashier_pin)
    val deleteLabel = stringResource(R.string.woopos_security_delete_cashier_pin)
    val deleteColor = WooPosTheme.colors.onSurfaceError
    if (stacked) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)) {
            WooPosOutlinedButtonSmall(
                modifier = Modifier.semantics { contentDescription = changeLabel },
                text = change,
                onClick = onChange,
            )
            WooPosOutlinedButtonSmall(
                modifier = Modifier.semantics { contentDescription = deleteLabel },
                text = delete,
                onClick = onDelete,
            )
        }
    } else {
        WooPosOverflowMenu(
            contentDescription = stringResource(R.string.woopos_security_cashier_pin_actions),
            items = listOf(
                WooPosOverflowMenuItem(label = change, contentDescription = changeLabel, onClick = onChange),
                WooPosOverflowMenuItem(
                    label = delete,
                    color = deleteColor,
                    contentDescription = deleteLabel,
                    onClick = onDelete,
                ),
            )
        )
    }
}

@Composable
private fun LockSettingsCard() {
    // TODO WOOMOB-4603: Persist these choices and apply the lock triggers.
    var onLaunch by rememberSaveable { mutableStateOf(true) }
    var onBackground by rememberSaveable { mutableStateOf(true) }
    var afterSale by rememberSaveable { mutableStateOf(false) }
    var inactivityMinutes by rememberSaveable { mutableIntStateOf(5) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    SecurityCard(stringResource(R.string.woopos_security_lock_settings)) {
        SecurityToggle(stringResource(R.string.woopos_security_lock_on_launch), onLaunch, { onLaunch = it })
        SecurityDivider()
        SecurityToggle(stringResource(R.string.woopos_security_lock_on_background), onBackground, { onBackground = it })
        SecurityDivider()
        SecurityToggle(stringResource(R.string.woopos_security_lock_after_sale), afterSale, { afterSale = it })
        SecurityDivider()
        val selectionLabel = stringResource(
            R.string.woopos_security_lock_after_selection,
            inactivityLabel(inactivityMinutes),
        )
        SecurityRow(
            title = stringResource(R.string.woopos_security_lock_after),
            modifier = Modifier.clickable(
                role = Role.Button,
                onClickLabel = selectionLabel,
                onClick = { pickerOpen = true },
            ).padding(vertical = WooPosSpacing.Small.value),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value),
                ) {
                    WooPosText(
                        inactivityLabel(inactivityMinutes),
                        style = WooPosTypography.BodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_down),
                        contentDescription = null,
                        modifier = Modifier.size(WooPosIconSize.XSmall.value),
                    )
                }
                DropdownMenu(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    expanded = pickerOpen,
                    onDismissRequest = { pickerOpen = false },
                ) {
                    INACTIVITY_OPTIONS.forEach { minutes ->
                        DropdownMenuItem(
                            modifier = Modifier.semantics { selected = minutes == inactivityMinutes },
                            text = { WooPosText(inactivityLabel(minutes), style = WooPosTypography.BodyMedium) },
                            onClick = {
                                inactivityMinutes = minutes
                                pickerOpen = false
                            },
                            trailingIcon = if (minutes == inactivityMinutes) {
                                {
                                    Icon(
                                        imageVector = WooPosIcons.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(WooPosIconSize.XSmall.value),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun securityTextButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = WooPosTheme.colors.textLink,
)

@Composable
private fun inactivityLabel(minutes: Int): String = when (minutes) {
    0 -> stringResource(R.string.woopos_security_never)
    else -> pluralStringResource(R.plurals.woopos_security_minutes, minutes, minutes)
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityScreenPreview() {
    SecurityScreenPreview(WooPosSettingsSecurityState.Off)
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityScreenProtectedPreview() {
    SecurityScreenPreview(WooPosSettingsSecurityState.Protected(hasCashierPIN = false, removalFailed = false))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityScreenCashierSetPreview() {
    SecurityScreenPreview(WooPosSettingsSecurityState.Protected(hasCashierPIN = true, removalFailed = true))
}

@Composable
private fun SecurityScreenPreview(state: WooPosSettingsSecurityState) {
    WooPosTheme {
        WooPosSettingsSecurityScreen(
            state,
            onSetUpPIN = {},
            onProtectionTurnedOff = {},
            onChangePIN = {},
            onDeleteCashierPIN = {},
        )
    }
}

private val ROW_MIN_WIDTH = 300.dp
private val INACTIVITY_OPTIONS = listOf(0, 1, 2, 5, 10, 15)
