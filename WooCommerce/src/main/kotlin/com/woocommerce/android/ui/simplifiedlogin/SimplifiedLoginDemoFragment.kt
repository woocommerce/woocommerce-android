package com.woocommerce.android.ui.simplifiedlogin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.simplifiedlogin.data.AccountRepository
import com.woocommerce.android.ui.simplifiedlogin.data.WPCom2FAResult
import com.woocommerce.android.ui.simplifiedlogin.data.WPComLoginResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SimplifiedLoginDemoFragment : BaseFragment() {
    @Inject
    lateinit var accountRepository: AccountRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireActivity()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    SimplifiedLoginDemo(accountRepository, findNavController())
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SimplifiedLoginDemo(accountRepository: AccountRepository, navController: NavController) {
    var is2Fa by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totpCode by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        if (!is2Fa) {
            WCOutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                label = "Email",
                modifier = Modifier.autofill(
                    autofillTypes = listOf(AutofillType.Username, AutofillType.EmailAddress),
                    onFill = {
                        email = it
                    }
                )
            )

            WCPasswordField(
                value = password, onValueChange = {
                    password = it
                },
                label = "Password",
                modifier = Modifier.autofill(
                    autofillTypes = listOf(AutofillType.Password),
                    onFill = {
                        password = it
                    }
                )
            )
        } else {
            WCOutlinedTextField(
                value = totpCode.orEmpty(),
                onValueChange = {
                    totpCode = it
                },
                label = "TOTP Code"
            )
        }


        Spacer(modifier = Modifier.weight(1f))
        WCColoredButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    if (!is2Fa) {
                        when (val result = accountRepository.login(email, password)) {
                            is WPComLoginResult.Success -> navController.navigate(
                                NavGraphMainDirections
                                    .actionGlobalLoginToSitePickerFragment(true),
                                // After finishing login, we want to clear the other fragments from the backstack
                                NavOptions.Builder()
                                    .setPopUpTo(navController.graph.startDestinationId, true)
                                    .build()
                            )
                            is WPComLoginResult.Requires2FA -> {
                                is2Fa = true
                            }
                            else -> println(result)
                        }
                    } else {
                        when (val twoStepResult = accountRepository.submitTwoStepCode(email, password, totpCode!!)) {
                            is WPCom2FAResult.Success -> navController.navigate(
                                NavGraphMainDirections
                                    .actionGlobalLoginToSitePickerFragment(true),
                                // After finishing login, we want to clear the other fragments from the backstack
                                NavOptions.Builder()
                                    .setPopUpTo(navController.graph.startDestinationId, true)
                                    .build()
                            )
                            else -> println(twoStepResult)
                        }
                    }
                    isLoading = false
                }
            },
            enabled = when (is2Fa) {
                true -> totpCode.isNotNullOrEmpty()
                false -> email.isNotEmpty() && password.isNotEmpty()
            }
        ) {
            Text(text = "Login")
        }
    }

    if (isLoading) {
        ProgressDialog(title = "", subtitle = "Signing in...")
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofill(
    autofillTypes: List<AutofillType>,
    onFill: ((String) -> Unit),
) = composed {
    val autofill = LocalAutofill.current
    val autofillNode = AutofillNode(onFill = onFill, autofillTypes = autofillTypes)
    LocalAutofillTree.current += autofillNode

    this
        .onGloballyPositioned {
            autofillNode.boundingBox = it.boundsInWindow()
        }
        .onFocusChanged { focusState ->
            autofill?.run {
                if (focusState.isFocused) {
                    requestAutofillForNode(autofillNode)
                } else {
                    cancelAutofillForNode(autofillNode)
                }
            }
        }
}


