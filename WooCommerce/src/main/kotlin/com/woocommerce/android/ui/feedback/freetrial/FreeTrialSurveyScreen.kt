package com.woocommerce.android.ui.feedback.freetrial

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun FreeTrialSurvey(viewModel: FreeTrialSurveyViewModel) {
    viewModel.surveyOptions.observeAsState().value?.let { state ->
        Scaffold(topBar = {
            Toolbar(
                title = { Text("") },
                navigationIcon = Filled.ArrowBack,
                onNavigationButtonClick = viewModel::onArrowBackPressed,
            )
        }) { padding ->
            Text(
                modifier = Modifier.padding(padding),
                text = ("FreeTrialSurvey")
            )
        }
    }
}
