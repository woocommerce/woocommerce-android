package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Test

class WooTopAppBarActionTest {
    @Test
    fun `given blank icon content description, when creating icon action, then throw`() {
        assertThatIllegalArgumentException().isThrownBy {
            WooTopAppBarAction.Icon(
                imageVector = TEST_ICON,
                contentDescription = " ",
                onClick = {},
            )
        }
    }

    @Test
    fun `given blank text action label, when creating text action, then throw`() {
        assertThatIllegalArgumentException().isThrownBy {
            WooTopAppBarAction.Text(
                text = " ",
                onClick = {},
            )
        }
    }

    private companion object {
        val TEST_ICON = ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).build()
    }
}
