package com.woocommerce.android.ui.products.images.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@AndroidEntryPoint
class ProductImageRemoveBackgroundFragment : BaseFragment() {

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: ProductImageRemoveBackgroundViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    Surface {
                        ProductImageRemoveBackgroundScreen(viewModel.state.collectAsState())
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ProductImageRemoveBackgroundViewModel.ExitScreen -> {
                    findNavController().navigateUp()
                }
                is MultiLiveEvent.Event.ShowSnackbar -> {
                    uiMessageResolver.showSnack(event.message)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.state.collect {
                when (it) {
                    is ViewState.BackgroundProcessingInProgress -> {
                        // Show loading state if needed
                    }
                    is ViewState.Success -> {
                        // Handle the completion of background processing
                        // For example, you might want to show the processed image
                    }
                    is ViewState.Failure -> {
                        uiMessageResolver.showSnack(R.string.error_generic)
                    }

                    ViewState.ImageUploadInProgress -> {
                    }
                }
            }
        }
    }
}

@Composable
fun ProductImageRemoveBackgroundScreen(state: State<ViewState>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.remove_background)) },
                backgroundColor = MaterialTheme.colors.surface
            )
        }
    ) { paddingValues ->
        when (val viewState = state.value) {
            is ViewState.Success -> {
                AsyncImage(
                    model = viewState.bitmap,
                    contentDescription = stringResource(R.string.product_image_content_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            is ViewState.BackgroundProcessingInProgress -> {
                Box(modifier = Modifier.padding(paddingValues)) {
                    AsyncImage(
                        model = viewState.imageUri,
                        contentDescription = stringResource(R.string.product_image_content_description),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    MagicSparkles()
                }
            }

            ViewState.Failure -> {
                Text("Failure!")
            }

            ViewState.ImageUploadInProgress -> {
                Text("Uploading image...")
            }
        }
    }
}

@Composable
fun MagicSparkles() {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle_transition")

    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "master_animation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val sparkleEvents = generateSparkleEvents(size.width.toInt(), size.height.toInt())

        sparkleEvents.forEach { sparkle ->
            val sparkleProgress = ((animationProgress - sparkle.startTime + 1f) % 1f)

            if (sparkleProgress >= 0f && sparkleProgress <= sparkle.duration) {
                val normalizedProgress = sparkleProgress / sparkle.duration

                val pulseAlpha = when {
                    normalizedProgress < 0.3f -> normalizedProgress / 0.3f // Fade in
                    normalizedProgress > 0.7f -> 1f - ((normalizedProgress - 0.7f) / 0.3f) // Fade out
                    else -> 1f
                }

                val pulseSize = when {
                    normalizedProgress < 0.2f -> normalizedProgress / 0.2f
                    normalizedProgress > 0.8f -> 1f - ((normalizedProgress - 0.8f) / 0.2f) * 0.5f
                    else -> 1f + sin(normalizedProgress * Math.PI * 4).toFloat() * 0.2f
                }

                if (pulseAlpha > 0f && pulseSize > 0f) {
                    drawSparkle(
                        center = sparkle.position,
                        size = sparkle.baseSize * pulseSize,
                        color = sparkle.color.copy(alpha = sparkle.color.alpha * pulseAlpha),
                        shape = sparkle.shape
                    )
                }
            }
        }
    }
}

private enum class SparkleShape {
    STAR_4, HEART
}

private data class SparkleEvent(
    val position: Offset,
    val startTime: Float, // 0.0 to 1.0 within the 3-second cycle
    val duration: Float, // How long the sparkle lasts (0.0 to 1.0)
    val baseSize: Float,
    val color: Color,
    val shape: SparkleShape
)

private fun generateSparkleEvents(width: Int, height: Int): List<SparkleEvent> {
    val random = Random(42) // Fixed seed for consistent animation
    val sparkleCount = 15

    return (1..sparkleCount).map { index ->
        val colors = listOf(
            Color(0xFFAB87D5),
            Color(0xFFFFFFFF),
            Color(0xFFBD70F6),
            Color(0xFF3F1781),
            Color(0xFFBDA2FF),
            Color(0xFF9B47D0),
            Color(0xFF4B2A91),
            Color(0xFFBFA0FF),
            Color(0xFFC1A0DD),
            Color(0xFF6F5FCC),
            Color(0xFFE198FB),
            Color(0xFF6620D3)
        )

        val shapes = SparkleShape.entries.toTypedArray()

        SparkleEvent(
            position = Offset(
                x = random.nextFloat() * width,
                y = random.nextFloat() * height
            ),
            startTime = random.nextFloat(),
            duration = 0.15f + random.nextFloat() * 0.4f,
            baseSize = (6 + random.nextInt(20)) * 2.5f,
            color = colors[random.nextInt(colors.size)],
            shape = shapes[random.nextInt(shapes.size)]
        )
    }
}

private fun DrawScope.drawSparkle(
    center: Offset,
    size: Float,
    color: Color,
    shape: SparkleShape
) {
    when (shape) {
        SparkleShape.STAR_4 -> drawStar(center, size, color, 4)
        SparkleShape.HEART -> drawHeart(center, size, color)
    }
}

private fun DrawScope.drawStar(center: Offset, size: Float, color: Color, rays: Int) {
    val path = Path()

    for (i in 0 until rays * 2) {
        val angle = (i * Math.PI / rays).toFloat()
        val radius = if (i % 2 == 0) size else size * 0.4f

        val x = center.x + cos(angle) * radius
        val y = center.y + sin(angle) * radius

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawHeart(center: Offset, size: Float, color: Color) {
    val path = Path()
    val scale = size / 30f
    val numPoints = 100

    var isFirst = true
    for (i in 0..numPoints) {
        val t = (i * 2 * Math.PI / numPoints).toFloat()

        // Parametric heart equations:
        val heartX = 16 * sin(t) * sin(t) * sin(t)
        val heartY = 13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)

        val x = center.x + heartX * scale
        val y = center.y - heartY * scale

        if (isFirst) {
            path.moveTo(x, y)
            isFirst = false
        } else {
            path.lineTo(x, y)
        }
    }

    path.close()
    drawPath(path, color)
}

@Preview
@Composable
fun ProductImageRemoveBackgroundScreenPreview() {
    WooThemeWithBackground {
        val image = Product.Image(
            id = 1L,
            source = "https://ma.tt/",
            name = "Sample Image",
            isCoverImage = true,
            dateCreated = Date.from(Instant.now()),
        )
        val state = remember {
            mutableStateOf(ViewState.BackgroundProcessingInProgress(image.source.toUri()))
        }
        ProductImageRemoveBackgroundScreen(state)
    }
}

@Preview
@Composable
fun MagicSparklesPreview() {
    WooThemeWithBackground {
        Surface(color = Color.Black.copy(alpha = 0.3f)) {
            MagicSparkles()
        }
    }
}
