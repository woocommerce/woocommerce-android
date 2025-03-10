package com.woocommerce.android.ui.orders.wooshippinglabels.address

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarDefaults
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCModalBottomSheet
import com.woocommerce.android.ui.compose.component.dismissWCModalBottomSheet
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.RoundedCornerBoxWithBorder
import com.woocommerce.android.ui.orders.wooshippinglabels.ShipmentDetailsSectionTitle
import com.woocommerce.android.ui.orders.wooshippinglabels.components.RoundedBorderDropDownWithLabel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.shippingSelectedBackgroundColor
import kotlinx.coroutines.launch

@Composable
fun WooShippingEditAddressScreen(
    viewModel: WooShippingEditAddressViewModel,
    modifier: Modifier = Modifier
) {
    val viewState = viewModel.viewState.collectAsState().value
    WooShippingEditAddressScreen(
        screenTitle = viewModel.screenTitle,
        editableAddress = viewState.editableAddress,
        isCompanyExpanded = viewState.isCompanyExpanded,
        loading = viewState.loading,
        error = viewState.error,
        shouldUseStatesInput = viewState.shouldUseStatesInput,
        addressStatus = viewState.addressStatus,
        addressValidationState = viewState.addressValidationState,
        onAddressSelectionChange = viewModel::onAddressSelectionChange,
        onCloseAddressSelection = viewModel::onCloseAddressSelection,
        onExpandCompany = viewModel::onExpandCompany,
        onNameChange = viewModel::onNameChange,
        onCompanyChange = viewModel::onCompanyChange,
        onAddressChange = viewModel::onAddressChange,
        onCityChange = viewModel::onCityChange,
        onPostalCodeChange = viewModel::onPostalCodeChange,
        onEmailChange = viewModel::onEmailChange,
        onPhoneChange = viewModel::onPhoneChange,
        onCountryChange = viewModel::onCountryChange,
        onRawStateChange = viewModel::onRawStateChange,
        onStateChange = viewModel::onStateChange,
        onNormalizeAddress = viewModel::onNormalizeAddress,
        onUpdateAddress = viewModel::onUpdateAddress,
        onUpdateNormalizedAddress = viewModel::onUpdateNormalizedAddress,
        onNavigateBack = viewModel::onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooShippingEditAddressScreen(
    screenTitle: String,
    editableAddress: EditableAddress,
    loading: WooShippingEditAddressViewModel.LoadingState,
    error: WooShippingEditAddressViewModel.EditAddressError?,
    shouldUseStatesInput: Boolean,
    isCompanyExpanded: Boolean,
    addressStatus: AddressStatus,
    addressValidationState: AddressValidationState,
    onAddressSelectionChange: (AddressValidationState.AddressSelection) -> Unit,
    onCloseAddressSelection: () -> Unit,
    onExpandCompany: () -> Unit,
    onNameChange: (String) -> Unit,
    onCompanyChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onPostalCodeChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onCountryChange: () -> Unit,
    onRawStateChange: (String) -> Unit,
    onStateChange: () -> Unit,
    onNormalizeAddress: (editableAddress: EditableAddress) -> Unit,
    onUpdateAddress: (editableAddress: EditableAddress) -> Unit,
    onUpdateNormalizedAddress: (selection: AddressValidationState.AddressSelection) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    modifier = Modifier.padding(bottom = 120.dp)
                )
            }
        },
        topBar = {
            Toolbar(
                title = screenTitle,
                onNavigationButtonClick = onNavigateBack,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { padding ->
        Column(modifier = modifier.fillMaxSize()) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp)
                    .weight(1f)
            ) {
                val companyFocusRequester = remember { FocusRequester() }
                val addressFocusRequester = remember { FocusRequester() }
                val cityFocusRequester = remember { FocusRequester() }
                val postalCodeFocusRequester = remember { FocusRequester() }
                val emailFocusRequester = remember { FocusRequester() }
                val phoneFocusRequester = remember { FocusRequester() }
                val stateFocusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current

                RoundedBorderTextFieldWithLabel(
                    label = stringResource(id = R.string.woo_shipping_label_name),
                    text = editableAddress.name.value,
                    error = editableAddress.name.error,
                    isRequired = editableAddress.name.isRequired,
                    onTextChange = onNameChange,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (isCompanyExpanded) {
                                companyFocusRequester.requestFocus()
                            } else {
                                addressFocusRequester.requestFocus()
                            }
                        }
                    ),
                )

                CollapsedField(
                    isExpanded = isCompanyExpanded,
                    label = stringResource(id = R.string.woo_shipping_label_company),
                    onExpand = onExpandCompany,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RoundedBorderTextFieldWithLabel(
                        label = stringResource(id = R.string.woo_shipping_label_company),
                        text = editableAddress.company.value,
                        isRequired = editableAddress.company.isRequired,
                        onTextChange = onCompanyChange,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                addressFocusRequester.requestFocus()
                            }
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                        focusRequester = companyFocusRequester
                    )
                }

                RoundedBorderDropDownWithLabel(
                    label = "${stringResource(id = R.string.woo_shipping_label_country)} *",
                    text = editableAddress.country.name,
                    modifier = Modifier.padding(top = 24.dp),
                    onClick = onCountryChange
                )
                RoundedBorderTextFieldWithLabel(
                    label = stringResource(id = R.string.woo_shipping_label_address),
                    text = editableAddress.address.value,
                    error = editableAddress.address.error,
                    isRequired = editableAddress.address.isRequired,
                    onTextChange = onAddressChange,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            cityFocusRequester.requestFocus()
                        }
                    ),
                    focusRequester = addressFocusRequester,
                    modifier = Modifier.padding(top = 8.dp)
                )
                RoundedBorderTextFieldWithLabel(
                    label = stringResource(id = R.string.woo_shipping_label_city),
                    text = editableAddress.city.value,
                    error = editableAddress.city.error,
                    isRequired = editableAddress.city.isRequired,
                    onTextChange = onCityChange,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (shouldUseStatesInput) {
                                stateFocusRequester.requestFocus()
                            } else {
                                postalCodeFocusRequester.requestFocus()
                            }
                        }
                    ),
                    focusRequester = cityFocusRequester,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row {
                    if (shouldUseStatesInput) {
                        RoundedBorderTextFieldWithLabel(
                            label = stringResource(id = R.string.woo_shipping_label_state),
                            text = editableAddress.state.name,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    postalCodeFocusRequester.requestFocus()
                                }
                            ),
                            focusRequester = stateFocusRequester,
                            onTextChange = onRawStateChange,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .weight(1f)
                        )
                    } else {
                        RoundedBorderDropDownWithLabel(
                            label = stringResource(id = R.string.woo_shipping_label_state),
                            text = editableAddress.state.name,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .weight(1f),
                            onClick = onStateChange
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    RoundedBorderTextFieldWithLabel(
                        label = stringResource(id = R.string.woo_shipping_label_post_code),
                        text = editableAddress.postalCode.value,
                        error = editableAddress.postalCode.error,
                        isRequired = editableAddress.postalCode.isRequired,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                emailFocusRequester.requestFocus()
                            }
                        ),
                        focusRequester = postalCodeFocusRequester,
                        onTextChange = onPostalCodeChange,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .weight(1f)
                    )
                }

                RoundedBorderTextFieldWithLabel(
                    label = stringResource(id = R.string.woo_shipping_label_email),
                    text = editableAddress.email.value,
                    error = editableAddress.email.error,
                    isRequired = editableAddress.email.isRequired,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    onTextChange = onEmailChange,
                    keyboardActions = KeyboardActions(
                        onNext = {
                            phoneFocusRequester.requestFocus()
                        }
                    ),
                    focusRequester = emailFocusRequester,
                    modifier = Modifier.padding(top = 32.dp)
                )
                RoundedBorderTextFieldWithLabel(
                    label = stringResource(id = R.string.woo_shipping_label_phone),
                    text = editableAddress.phone.value,
                    error = editableAddress.phone.error,
                    isRequired = editableAddress.phone.isRequired,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            phoneFocusRequester.freeFocus()
                            keyboardController?.hide()
                        }
                    ),
                    focusRequester = phoneFocusRequester,
                    onTextChange = onPhoneChange,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Surface(elevation = 8.dp) {
                AddressStatusSection(
                    editableAddress = editableAddress,
                    addressStatus = addressStatus,
                    onNormalizeAddress = onNormalizeAddress,
                    onUpdateAddress = onUpdateAddress,
                    onClose = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        val modalSheetState = androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        val coroutineScope = rememberCoroutineScope()

        val addressSelection = when (addressValidationState) {
            is AddressValidationState.AddressSelection -> addressValidationState
            is AddressValidationState.NormalizedAddressUpdateFailed -> addressValidationState.selection
            else -> null
        }

        var isBottomSheetSnackBarVisible by remember { mutableStateOf(false) }

        LaunchedEffect(addressSelection) {
            if (addressSelection != null) {
                coroutineScope.launch { modalSheetState.show() }.invokeOnCompletion {
                    if (modalSheetState.isVisible) {
                        isBottomSheetSnackBarVisible = true
                    }
                }
            } else {
                isBottomSheetSnackBarVisible = false
                modalSheetState.hide()
            }
        }

        val retry = stringResource(id = R.string.retry)
        if (addressSelection != null) {
            WCModalBottomSheet(
                sheetState = modalSheetState,
                onDismissRequest = { onCloseAddressSelection() },
                contentWindowInsets = { WindowInsets.statusBars }
            ) {
                SelectAddressWithCustomSnackBar(
                    addressSelection = addressSelection,
                    onAddressSelectionChange = onAddressSelectionChange,
                    onUpdateNormalizedAddress = onUpdateNormalizedAddress,
                    onCloseAddressSelection = onCloseAddressSelection,
                    error = error,
                    isBottomSheetSnackBarVisible = isBottomSheetSnackBarVisible,
                    modalSheetState = modalSheetState
                )
            }
        } else {
            LaunchedEffect(error) {
                if (error != null) {
                    val result = snackbarHostState.showSnackbar(
                        message = error.message,
                        duration = SnackbarDuration.Indefinite,
                        actionLabel = retry
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> {}
                        SnackbarResult.ActionPerformed -> {
                            error.onRetry()
                        }
                    }
                }
            }
        }
    }
    if (loading is WooShippingEditAddressViewModel.LoadingState.DisplayLoading) {
        LoadingModal(
            title = loading.title,
            description = loading.message
        )
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
internal fun AddressStatusSection(
    editableAddress: EditableAddress,
    addressStatus: AddressStatus,
    onNormalizeAddress: (editableAddress: EditableAddress) -> Unit,
    onUpdateAddress: (editableAddress: EditableAddress) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val buttonText = when (addressStatus) {
            AddressStatus.VERIFIED -> stringResource(id = R.string.close)
            AddressStatus.UNVERIFIED -> stringResource(id = R.string.woo_shipping_address_validate_and_save)
            AddressStatus.MISSING_INFO -> stringResource(id = R.string.woo_shipping_address_missing_info_hint)
            AddressStatus.SAVE_CHANGES -> stringResource(id = R.string.woo_shipping_address_save_changes)
            AddressStatus.MISSING_ADDRESS -> ""
        }

        val buttonAction: () -> Unit = when (addressStatus) {
            AddressStatus.VERIFIED -> {
                { onClose() }
            }

            AddressStatus.UNVERIFIED -> {
                { onNormalizeAddress(editableAddress) }
            }

            AddressStatus.MISSING_INFO -> {
                {}
            }

            AddressStatus.SAVE_CHANGES -> {
                { onUpdateAddress(editableAddress) }
            }
            AddressStatus.MISSING_ADDRESS -> {
                {}
            }
        }

        AddressStatusIndicator(
            addressStatus = addressStatus,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        WCColoredButton(
            onClick = buttonAction,
            enabled = addressStatus != AddressStatus.MISSING_INFO,
            text = buttonText,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun RoundedBorderTextFieldWithLabel(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    hint: String = "",
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null,
    endSection: @Composable () -> Unit = {}
) {
    val hasError = error.isNotNullOrEmpty()
    val color = if (hasError) {
        MaterialTheme.colors.error
    } else {
        colorResource(R.color.divider_color)
    }

    val textFieldModifier = if (focusRequester != null) {
        Modifier
            .focusable()
            .focusRequester(focusRequester)
    } else {
        Modifier
    }

    val labelWithRequired = if (isRequired) "$label *" else label

    Column(modifier = modifier) {
        Text(
            text = labelWithRequired,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        RoundedCornerBoxWithBorder(
            borderColor = color
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        modifier = textFieldModifier.fillMaxWidth(),
                        value = text,
                        onValueChange = onTextChange,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),
                    )
                    if (text.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.body2,
                            color = colorResource(id = R.color.color_on_surface_disabled)
                        )
                    }
                }
                endSection()
            }
        }
        error?.let {
            Text(
                text = error,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun RoundedBorderTextFieldWithLabelPreview() {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .padding(16.dp)
    ) {
        RoundedBorderTextFieldWithLabel(
            label = "Label",
            text = "123",
            onTextChange = {},
            hint = "Hint"
        )
        Spacer(modifier = Modifier.padding(8.dp))
        RoundedBorderTextFieldWithLabel(
            label = "Empty text field",
            text = "",
            onTextChange = {},
            hint = "Hint"
        )
        Spacer(modifier = Modifier.padding(8.dp))
        RoundedBorderTextFieldWithLabel(
            label = "Required field",
            text = "",
            onTextChange = {},
            hint = "Hint",
            error = "This field is required"
        )
    }
}

@Composable
private fun CollapsedField(
    label: String,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedContent(targetState = isExpanded, label = "expand_field_animation") { value ->
        if (value) {
            content()
        } else {
            Row(
                modifier = modifier
                    .clickable { onExpand() }
                    .padding(vertical = 16.dp, horizontal = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    text = stringResource(id = R.string.add, label.lowercase()),
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectAddressWithCustomSnackBar(
    addressSelection: AddressValidationState.AddressSelection,
    onAddressSelectionChange: (AddressValidationState.AddressSelection) -> Unit,
    onUpdateNormalizedAddress: (selection: AddressValidationState.AddressSelection) -> Unit,
    onCloseAddressSelection: () -> Unit,
    error: WooShippingEditAddressViewModel.EditAddressError?,
    isBottomSheetSnackBarVisible: Boolean,
    modalSheetState: SheetState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        val coroutineScope = rememberCoroutineScope()

        SelectAddress(
            addressSelection = addressSelection,
            onAddressSelectionChange = onAddressSelectionChange,
            onUpdateNormalizedAddress = onUpdateNormalizedAddress,
            onCloseAddressSelection = {
                dismissWCModalBottomSheet(
                    coroutineScope = coroutineScope,
                    modalSheetState = modalSheetState,
                    invokeOnCompletion = onCloseAddressSelection
                )
            }
        )

        if (error != null) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isBottomSheetSnackBarVisible,
                enter = fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 180
                    )
                ) + scaleIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 180
                    )
                ),
                exit = fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 90
                    )
                ) + scaleOut(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 90
                    )

                )
            ) {
                Box(modifier = Modifier.padding(bottom = 120.dp)) {
                    Snackbar(
                        modifier = Modifier.padding(12.dp),
                        content = { Text(error.message) },
                        action = {
                            TextButton(
                                colors = ButtonDefaults
                                    .textButtonColors(contentColor = SnackbarDefaults.primaryActionColor),
                                onClick = { error.onRetry() },
                                content = { Text(stringResource(id = R.string.retry)) }
                            )
                        },
                        actionOnNewLine = false,
                        shape = MaterialTheme.shapes.small,
                        backgroundColor = SnackbarDefaults.backgroundColor,
                        contentColor = MaterialTheme.colors.surface,
                        elevation = 6.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectAddress(
    addressSelection: AddressValidationState.AddressSelection,
    onAddressSelectionChange: (AddressValidationState.AddressSelection) -> Unit,
    onUpdateNormalizedAddress: (selection: AddressValidationState.AddressSelection) -> Unit,
    onCloseAddressSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        IconButton(onClick = onCloseAddressSelection) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface
            )
        }
        Column(
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 16.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(id = R.string.woo_shipping_confirm_address_title),
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(bottom = 16.dp),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = R.string.woo_shipping_confirm_address_description),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clickable { onCloseAddressSelection() }
            )
            ShipmentDetailsSectionTitle(
                title = stringResource(id = R.string.woo_shipping_confirm_address_entered),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AddressSelectionItem(
                address = addressSelection.addressNormalization.address,
                isSelected = addressSelection.selectedAddress == addressSelection.addressNormalization.address,
                onClick = {
                    onAddressSelectionChange(
                        addressSelection.copy(selectedAddress = addressSelection.addressNormalization.address)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            ShipmentDetailsSectionTitle(
                title = stringResource(id = R.string.woo_shipping_confirm_address_suggested),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AddressSelectionItem(
                address = addressSelection.addressNormalization.normalizedAddress,
                isSelected =
                addressSelection.selectedAddress == addressSelection.addressNormalization.normalizedAddress,
                onClick = {
                    onAddressSelectionChange(
                        addressSelection.copy(selectedAddress = addressSelection.addressNormalization.normalizedAddress)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        val buttonText = if (addressSelection.selectedAddress == addressSelection.addressNormalization.address) {
            stringResource(id = R.string.woo_shipping_confirm_submit_address_entered)
        } else {
            stringResource(id = R.string.woo_shipping_confirm_submit_address_suggested)
        }

        Surface(elevation = 8.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                WCColoredButton(
                    onClick = { onUpdateNormalizedAddress(addressSelection) },
                    text = buttonText,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AddressSelectionItem(
    address: Address,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        colorResource(R.color.divider_color)
    }

    val backgroundColor = if (isSelected) {
        animateColorAsState(
            targetValue = MaterialTheme.colors.shippingSelectedBackgroundColor,
            label = "colorAnimation"
        )
    } else {
        animateColorAsState(targetValue = MaterialTheme.colors.surface, label = "colorAnimation")
    }

    RoundedCornerBoxWithBorder(
        modifier = modifier,
        innerModifier = Modifier
            .clickable { onClick() }
            .padding(dimensionResource(id = R.dimen.major_100)),
        borderColor = borderColor,
        backgroundColor = backgroundColor.value
    ) {
        Text(
            text = address.toString(),
            modifier = Modifier
        )
    }
}

@Composable
fun LoadingModal(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colors.surface,
            shape = RoundedCornerShape(CornerSize(8.dp)),
            elevation = 4.dp,
            modifier = Modifier
                .sizeIn(maxWidth = 550.dp)
                .fillMaxWidth()
                .padding(64.dp)

        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6
                )
                Row(modifier = Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterVertically))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.PIXEL)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun LoadingModalPreview() {
    WooThemeWithBackground {
        LoadingModal(title = "Loading", description = "Please wait")
    }
}
