package com.movtery.zalithlauncher.ui.screens.game.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.theme.onCardColor

@Composable
fun SpeedrunTimerSection(
    modifier: Modifier = Modifier
) {
    val state = SpeedrunTimerState

    DisposableEffect(Unit) {
        state.enable()
        onDispose { state.disable() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = stringResource(R.string.speedrun_timer),
            color = onCardColor(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        val mcFont = FontFamily.Monospace

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.speedrun_rta),
                color = onCardColor().copy(alpha = 0.7f),
                fontSize = 9.sp
            )
            Text(
                text = state.formatTime(state.rtaMs),
                color = onCardColor(),
                fontFamily = mcFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.speedrun_igt),
                color = onCardColor().copy(alpha = 0.7f),
                fontSize = 9.sp
            )
            Text(
                text = state.formatTime(state.igtMs),
                color = onCardColor().copy(alpha = 0.85f),
                fontFamily = mcFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        state.currentWorld?.let { world ->
            Text(
                text = "${stringResource(R.string.speedrun_world)}: $world",
                color = onCardColor().copy(alpha = 0.5f),
                fontSize = 8.sp,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val btnModifier = Modifier.weight(1f)
            val btnColor = onCardColor().copy(alpha = 0.7f)

            if (state.isRunning) {
                TextButton(
                    modifier = btnModifier,
                    onClick = { state.stopTimer() }
                ) {
                    Text(
                        text = stringResource(R.string.speedrun_stop),
                        color = btnColor,
                        fontSize = 10.sp
                    )
                }
            } else {
                TextButton(
                    modifier = btnModifier,
                    onClick = { state.startTimer() }
                ) {
                    Text(
                        text = stringResource(R.string.speedrun_start),
                        color = btnColor,
                        fontSize = 10.sp
                    )
                }
            }

            TextButton(
                modifier = btnModifier,
                onClick = { state.resetTimer() }
            ) {
                Text(
                    text = stringResource(R.string.speedrun_reset),
                    color = btnColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}
