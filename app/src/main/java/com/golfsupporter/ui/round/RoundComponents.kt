package com.golfsupporter.ui.round

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfsupporter.util.ScoreLabel

/** Button-mode score card: [−] [value + name] [+] (PRD F-011A). */
@Composable
fun ButtonScoreCard(
    name: String,
    relative: Int,
    total: Int,
    onChange: (Int) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(64.dp))
            StepButton("−") { onChange(-1) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        ScoreLabel.shortLabel(relative),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScoreLabel.colorFor(relative),
                    )
                    Text(
                        ScoreLabel.nameFor(relative),
                        fontSize = 12.sp,
                        color = ScoreLabel.colorFor(relative),
                    )
                }
            }
            StepButton("+") { onChange(1) }
            Spacer(Modifier.width(8.dp))
            Text(
                ScoreLabel.formatTotal(total),
                modifier = Modifier.width(40.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Text(symbol, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Scroll-mode score card (PRD F-011B): vertical swipe changes the value.
 * Up = score decreases (under par), down = score increases (over par).
 * A drum-roll of prev / current / next values is shown, with a haptic tick on
 * each step (20dp per step by default).
 */
@Composable
fun ScrollScoreCard(
    name: String,
    relative: Int,
    total: Int,
    onChange: (Int) -> Unit,
    dpPerStep: Float = 20f,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val stepPx = with(density) { dpPerStep.dp.toPx() }
    var accumulated by remember { mutableFloatStateOf(0f) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(horizontal = 12.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { accumulated = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulated += dragAmount.y
                            while (accumulated <= -stepPx) {     // swipe up → decrease
                                onChange(-1)
                                accumulated += stepPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            while (accumulated >= stepPx) {       // swipe down → increase
                                onChange(1)
                                accumulated -= stepPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(64.dp))
            // Drum-roll column
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    ScoreLabel.shortLabel(relative - 1),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                )
                Text(
                    "${ScoreLabel.shortLabel(relative)}  ${ScoreLabel.nameFor(relative)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScoreLabel.colorFor(relative),
                )
                Text(
                    ScoreLabel.shortLabel(relative + 1),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                )
            }
            Text(
                ScoreLabel.formatTotal(total),
                modifier = Modifier.width(40.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}
