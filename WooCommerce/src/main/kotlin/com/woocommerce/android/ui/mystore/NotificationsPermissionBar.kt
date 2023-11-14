import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.main.MainActivityViewModel

@Composable
fun NotificationsPermissionCard(viewModel: MainActivityViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .background(colorResource(id = color.color_surface))
            .fillMaxWidth()
    ) {
        Divider(
            color = colorResource(id = color.divider_color),
            thickness = dimensionResource(id = dimen.minor_10)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = dimen.major_100))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(id = dimen.minor_100))
            ) {
                Image(
                    modifier = Modifier.padding(end = dimensionResource(id = dimen.minor_100)),
                    painter = painterResource(drawable.ic_megaphone),
                    contentDescription = "",
                )
                Text(
                    text = stringResource(id = R.string.notifications_permission_title),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = color.color_on_surface),
                )
            }
            Text(
                text = stringResource(id = R.string.notifications_permission_description),
                style = MaterialTheme.typography.body1,
                color = colorResource(id = color.color_on_surface),
                modifier = Modifier.padding(bottom = dimensionResource(id = dimen.major_100))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                WCTextButton(
                    modifier = Modifier.padding(end = dimensionResource(id = dimen.major_100)),
                    text = stringResource(id = R.string.dismiss),
                    onClick = viewModel::onNotificationsPermissionBarDismissButtonTapped,
                )
                WCTextButton(
                    text = stringResource(id = R.string.allow),
                    onClick = viewModel::onNotificationsPermissionBarAllowButtonTapped,
                )
            }
        }
    }
}

@Preview
@Composable
fun NotificationsPermissionCardPreview() {
    NotificationsPermissionCard()
}
