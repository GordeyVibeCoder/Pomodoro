package com.example.pomodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PomodoroTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PomodoroScreen()
                }
            }
        }
    }
}

enum class SessionType(val title: String, val minutes: Long) {
    FOCUS("Фокус", 25),
    SHORT_BREAK("Короткий перерыв", 5),
    LONG_BREAK("Длинный перерыв", 15)
}

@Composable
fun PomodoroScreen() {
    var sessionType by remember { mutableStateOf(SessionType.FOCUS) }
    var completedFocuses by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableLongStateOf(sessionType.minutes * 60) }

    fun switchSession(type: SessionType) {
        sessionType = type
        secondsLeft = type.minutes * 60
        isRunning = false
    }

    LaunchedEffect(isRunning, sessionType) {
        while (isRunning && secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
            if (secondsLeft == 0L) {
                if (sessionType == SessionType.FOCUS) {
                    completedFocuses += 1
                    val next = if (completedFocuses % 4 == 0) SessionType.LONG_BREAK else SessionType.SHORT_BREAK
                    switchSession(next)
                } else {
                    switchSession(SessionType.FOCUS)
                }
            }
        }
    }

    val totalSeconds = sessionType.minutes * 60f
    val progress = (secondsLeft / totalSeconds).coerceIn(0f, 1f)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Pomodoro + Lissajous", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(text = sessionType.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))

            LissajousFigure(
                modifier = Modifier
                    .size(260.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
                progress = progress,
                sessionType = sessionType
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = formatSeconds(secondsLeft),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(text = "Завершённых фокус-сессий: $completedFocuses")

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { isRunning = !isRunning }, modifier = Modifier.weight(1f)) {
                    Text(if (isRunning) "Пауза" else "Старт")
                }
                Button(onClick = { secondsLeft = sessionType.minutes * 60; isRunning = false }, modifier = Modifier.weight(1f)) {
                    Text("Сброс")
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                when (sessionType) {
                    SessionType.FOCUS -> switchSession(SessionType.SHORT_BREAK)
                    SessionType.SHORT_BREAK, SessionType.LONG_BREAK -> switchSession(SessionType.FOCUS)
                }
            }) {
                Text("Пропустить этап")
            }
        }
    }
}

@Composable
fun LissajousFigure(modifier: Modifier = Modifier, progress: Float, sessionType: SessionType) {
    val transition = rememberInfiniteTransition(label = "lissajous")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(4_000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "phase"
    )

    val (a, b, color) = when (sessionType) {
        SessionType.FOCUS -> Triple(3f, 2f, Color(0xFFEF5350))
        SessionType.SHORT_BREAK -> Triple(5f, 4f, Color(0xFF66BB6A))
        SessionType.LONG_BREAK -> Triple(7f, 5f, Color(0xFF42A5F5))
    }

    Canvas(modifier = modifier.padding(12.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radiusX = size.width * 0.42f * (0.65f + 0.35f * progress)
        val radiusY = size.height * 0.42f * (0.65f + 0.35f * progress)

        val points = 700
        val path = Path()

        repeat(points + 1) { i ->
            val t = i.toFloat() / points * (2f * PI.toFloat())
            val x = cx + radiusX * sin(a * t + phase)
            val y = cy + radiusY * sin(b * t)

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = color.copy(alpha = 0.9f), style = Stroke(width = 6f))

        val dotT = (1f - progress) * 2f * PI.toFloat()
        val dotX = cx + radiusX * sin(a * dotT + phase)
        val dotY = cy + radiusY * sin(b * dotT)
        drawCircle(color = color, radius = 10f, center = Offset(dotX, dotY))
        drawCircle(color = color.copy(alpha = 0.2f), radius = 18f, center = Offset(dotX, dotY))
    }
}

private fun formatSeconds(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
fun PomodoroTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
