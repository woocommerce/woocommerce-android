package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

/**
 * This is a layout that supports laying out Dialog's buttons according to the material guidelines, meaning:
 * - {Neutral}-----{Negative}-{Positive} when the Dialog's width fits the three buttons
 * - A stacked layout when the dialog's width doesn't fit them.
 */
@Composable
fun DialogButtonsRowLayout(
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    neutralButton: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    val measurePolicy = remember {
        object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                val childConstraints = constraints.copy(minWidth = 0)
                val confirmPlaceable =
                    measurables.first { it.layoutId == "confirm" }.measure(childConstraints)
                val dismissPlaceable =
                    measurables.first { it.layoutId == "dismiss" }.measure(childConstraints)
                val neutralPlaceable =
                    measurables.firstOrNull { it.layoutId == "neutral" }?.measure(childConstraints)

                val placeables = listOfNotNull(neutralPlaceable, dismissPlaceable, confirmPlaceable)

                val placeablesWidth = placeables.sumOf { it.width }

                val shouldStackItems = placeablesWidth > constraints.maxWidth
                val height = if (shouldStackItems) {
                    placeables.sumOf { it.height }
                } else {
                    placeables.maxOf { it.height }
                }

                val width = if (constraints.maxWidth != Constraints.Infinity) {
                    constraints.maxWidth
                } else {
                    placeables.sumOf { it.width }
                }

                return layout(width, height) {
                    if (!shouldStackItems) {
                        neutralPlaceable?.placeRelative(
                            x = 0,
                            y = (height - neutralPlaceable.height) / 2
                        )
                        confirmPlaceable.placeRelative(
                            x = width - confirmPlaceable.width,
                            y = (height - confirmPlaceable.height) / 2
                        )
                        dismissPlaceable.placeRelative(
                            x = width - confirmPlaceable.width - dismissPlaceable.width,
                            y = (height - dismissPlaceable.height) / 2
                        )
                    } else {
                        var yPosition = 0

                        placeables.forEach { placeable ->
                            placeable.placeRelative(
                                x = width - placeable.width,
                                y = yPosition
                            )
                            yPosition += placeable.height
                        }
                    }
                }
            }

            override fun IntrinsicMeasureScope.minIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int
            ): Int {
                return if (measurables.sumOf { it.maxIntrinsicWidth(Constraints.Infinity) } > width) {
                    measurables.sumOf { it.minIntrinsicHeight(width) }
                } else {
                    measurables.maxOf { it.minIntrinsicHeight(width) }
                }
            }

            override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int
            ): Int {
                return if (measurables.sumOf { it.maxIntrinsicWidth(Constraints.Infinity) } > width) {
                    measurables.sumOf { it.maxIntrinsicHeight(width) }
                } else {
                    measurables.maxOf { it.maxIntrinsicHeight(width) }
                }
            }

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int
            ): Int {
                return measurables.sumOf {
                    it.maxIntrinsicWidth(height)
                }
            }

            override fun IntrinsicMeasureScope.minIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int
            ): Int {
                return measurables.sumOf {
                    it.minIntrinsicWidth(height)
                }
            }
        }
    }
    Layout(
        content = {
            Box(Modifier.layoutId("confirm")) {
                confirmButton()
            }
            Box(Modifier.layoutId("dismiss")) {
                dismissButton()
            }
            neutralButton?.let {
                Box(Modifier.layoutId("neutral")) {
                    neutralButton()
                }
            }
        },
        measurePolicy = measurePolicy,
        modifier = modifier
    )
}

@Preview(widthDp = 300)
@Composable
private fun DialogButtonsRowLayoutPreview() {
    WooThemeWithBackground {
        Column {
            listOf(null, "Neutral", "A very long neutral button").forEach { neutralButton ->
                DialogButtonsRowLayout(
                    confirmButton = {
                        TextButton(onClick = { }) {
                            Text(stringResource(id = android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                    },
                    neutralButton = {
                        neutralButton?.let {
                            TextButton(onClick = { }) {
                                Text(neutralButton)
                            }
                        }
                    }
                )

                Divider()
            }
        }
    }
}
