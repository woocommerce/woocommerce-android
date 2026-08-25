package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class WooTopAppBarActionTest {
    @Test
    fun `given raw actions, when String-title overload is referenced, then it remains callable`() {
        val actions: @Composable RowScope.() -> Unit = {}
        val topAppBar: @Composable () -> Unit = {
            WooTopAppBar(title = "Product", actions = actions)
        }

        assertThat(topAppBar).isNotNull()
    }

    @Test
    fun `given blank icon content description, when creating icon action, then throw`() {
        assertThatThrownBy {
            WooTopAppBarAction.Icon(
                imageVector = TEST_ICON,
                contentDescription = " ",
                onClick = {},
            )
        }.isInstanceOf(AssertionError::class.java)
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
