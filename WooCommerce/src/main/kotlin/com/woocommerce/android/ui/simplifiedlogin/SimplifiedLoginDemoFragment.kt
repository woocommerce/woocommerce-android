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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCPasswordField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.simplifiedlogin.data.AccountRepository
import com.woocommerce.android.ui.simplifiedlogin.data.WPComLoginResult.Success
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

@Composable
private fun SimplifiedLoginDemo(accountRepository: AccountRepository, navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        WCOutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = "Email"
        )

        WCPasswordField(
            value = password, onValueChange = {
                password = it
            },
            label = "Password"
        )

        Spacer(modifier = Modifier.weight(1f))
        WCColoredButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    val result = accountRepository.login(email, password)
                    when (result) {
                        is Success -> navController.navigate(
                            NavGraphMainDirections
                                .actionGlobalLoginToSitePickerFragment(true),
                            // After finishing login, we want to clear the other fragments from the backstack
                            NavOptions.Builder()
                                .setPopUpTo(navController.graph.startDestinationId, true)
                                .build()
                        )
                        else -> println(result)
                    }
                    isLoading = false
                }
            }) {
            Text(text = "Login")
        }
    }

    if (isLoading) {
        ProgressDialog(title = "", subtitle = "Signing in...")
    }
}

