package com.woocommerce.android.ui.jetpack

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton

@Composable
fun JetpackActivationEligibilityErrorScreen() {
    Scaffold(
        topBar = {
            ToolbarWithHelpButton(
                navigationIcon = Icons.Filled.ArrowBack,
                onHelpButtonClick = { },
                onNavigationButtonClick = { }
            )
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colors.surface)
                .padding(paddingValues)
                .padding(vertical = dimensionResource(id = R.dimen.major_100))
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            ) {
                MainContent(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                )

                Spacer(Modifier.height(dimensionResource(id = R.dimen.major_100)))
                WCColoredButton(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                ) {
                    Text(text = stringResource(id = R.string.retry))
                }
            }
        }
    }
}

@Composable
private fun MainContent(modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Display name",
            style = MaterialTheme.typography.h5,
            color = colorResource(id = R.color.color_on_surface_high)
        )
        Text(
            text = "Shop Manager",
            style = MaterialTheme.typography.body1
        )

        Image(
            painter = painterResource(id = R.drawable.img_user_access_error),
            contentDescription = null,
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .weight(1f, fill = false)
        )
        Text(
            text = stringResource(id = R.string.jetpack_install_role_eligibility_error_message),
            textAlign = TextAlign.Center
        )

        WCTextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {}
        ) {
            Text(text = stringResource(id = R.string.jetpack_install_role_eligibility_learn_more))
        }
    }
}

@Preview
@Composable
private fun JetpackActivationEligibilityErrorScreenPreview() {
    JetpackActivationEligibilityErrorScreen()
}
