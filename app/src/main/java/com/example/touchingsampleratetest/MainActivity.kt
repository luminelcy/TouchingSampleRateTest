package com.example.touchingsampleratetest

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.LinkedList

class MainActivity : ComponentActivity() {
    private val timestamps = LinkedList<Long>()
    private val fullTimestamps = LinkedList<Long>()
    private val MAX_SAMPLES = 60
    private var updateSampleRate: ((Int) -> Unit)? = null
    private var updateFullSampleRate: ((Int) -> Unit)? = null
    private var updateDeviceInfo: ((String, Int) -> Unit)? = null
    private var useUnbuffered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var sampleRate by remember { mutableStateOf(0) }
            var fullSampleRate by remember { mutableStateOf(0) }
            var deviceName by remember { mutableStateOf("未检测到设备") }
            var deviceId by remember { mutableStateOf(-1) }
            var unbuffered by remember { mutableStateOf(false) }
            var showTrail by remember { mutableStateOf(false) }
            val trailPoints = remember { mutableStateListOf<Offset>() }

            updateSampleRate = { sampleRate = it }
            updateFullSampleRate = { fullSampleRate = it }
            updateDeviceInfo = { name, id ->
                deviceName = name
                deviceId = id
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().background(Color(0xFF000000)),
                contentAlignment = Alignment.Center
            ) {
                val screenWidthPx = constraints.maxWidth.toFloat()
                val density = LocalDensity.current.density
                val screenWidthDp = screenWidthPx / density
                val scale = (screenWidthDp / 800f).coerceIn(0.5f, 1.5f)
                val mainFontSize = (48 * scale).sp
                val labelFontSize = (24 * scale).sp
                val switchSpacing = (12 * scale).dp
                val sectionSpacing = (16 * scale).dp
                if (showTrail) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (useUnbuffered && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val syntheticEvent = MotionEvent.obtain(
                                            SystemClock.uptimeMillis(),
                                            SystemClock.uptimeMillis(),
                                            MotionEvent.ACTION_DOWN,
                                            down.position.x,
                                            down.position.y,
                                            0
                                        )
                                        window.decorView.requestUnbufferedDispatch(syntheticEvent)
                                        syntheticEvent.recycle()
                                    }
                                    trailPoints.clear()
                                    trailPoints.add(down.position)
                                    val now = SystemClock.elapsedRealtime()
                                    timestamps.clear()
                                    fullTimestamps.clear()
                                    timestamps.addLast(now)
                                    fullTimestamps.addLast(now)
                                    drag(down.id) { change ->
                                        trailPoints.add(change.position)
                                        if (trailPoints.size > MAX_SAMPLES) {
                                            trailPoints.removeAt(0)
                                        }
                                        val dragNow = SystemClock.elapsedRealtime()
                                        timestamps.addLast(dragNow)
                                        fullTimestamps.addLast(dragNow)
                                        while (timestamps.size > MAX_SAMPLES) timestamps.removeFirst()
                                        while (fullTimestamps.size > MAX_SAMPLES) fullTimestamps.removeFirst()
                                        if (timestamps.size >= 2) {
                                            val duration = (timestamps.last() - timestamps.first()).toDouble() / 1000.0
                                            updateSampleRate?.invoke((timestamps.size / duration).toInt())
                                        }
                                        if (fullTimestamps.size >= 2) {
                                            val fullDuration = (fullTimestamps.last() - fullTimestamps.first()).toDouble() / 1000.0
                                            updateFullSampleRate?.invoke((fullTimestamps.size / fullDuration).toInt())
                                        }
                                    }
                                    trailPoints.clear()
                                    timestamps.clear()
                                    fullTimestamps.clear()
                                }
                            }
                    ) {
                        val points = trailPoints.toList()
                        if (points.size >= 2) {
                            for (i in 1 until points.size) {
                                val alpha = i.toFloat() / points.size
                                drawLine(
                                    color = Color(0xFF00FF00).copy(alpha = alpha),
                                    start = points[i - 1],
                                    end = points[i],
                                    strokeWidth = 8f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                        for (i in points.indices) {
                            val alpha = (i + 1).toFloat() / points.size
                            drawCircle(
                                color = Color(0xFFFF0000).copy(alpha = alpha),
                                radius = 6f,
                                center = points[i]
                            )
                        }
                        if (points.isNotEmpty()) {
                            val last = points.last()
                            drawCircle(
                                color = Color(0xFF00FF00),
                                radius = 16f,
                                center = last
                            )
                            drawCircle(
                                color = Color(0xFFFFFFFF),
                                radius = 6f,
                                center = last
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${sampleRate} Hz (raw)\n${fullSampleRate} Hz (w/ history)\n${deviceName}\nDevice ID: ${deviceId}",
                        color = Color(0xFFFFFFFF),
                        fontSize = mainFontSize,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.padding(sectionSpacing))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Unbuffered",
                            color = Color(0xFFFFFFFF),
                            fontSize = labelFontSize,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(switchSpacing))
                        Switch(
                            checked = unbuffered,
                            onCheckedChange = {
                                unbuffered = it
                                useUnbuffered = it
                            },
                            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        )
                    }
                    Spacer(modifier = Modifier.height(switchSpacing))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Trail",
                            color = Color(0xFFFFFFFF),
                            fontSize = labelFontSize,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(switchSpacing))
                        Switch(
                            checked = showTrail,
                            onCheckedChange = {
                                showTrail = it
                                trailPoints.clear()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (useUnbuffered && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    window.decorView.requestUnbufferedDispatch(event)
                }
                timestamps.clear()
                fullTimestamps.clear()
                timestamps.addLast(event.eventTime)
                fullTimestamps.addLast(event.eventTime)
                val device = InputDevice.getDevice(event.deviceId)
                updateDeviceInfo?.invoke(device?.name ?: "未知设备", event.deviceId)
            }
            MotionEvent.ACTION_MOVE -> {
                val historySize = event.historySize
                for (i in 0 until historySize) {
                    fullTimestamps.addLast(event.getHistoricalEventTime(i))
                }
                timestamps.addLast(event.eventTime)
                fullTimestamps.addLast(event.eventTime)
                while (timestamps.size > MAX_SAMPLES) timestamps.removeFirst()
                while (fullTimestamps.size > MAX_SAMPLES) fullTimestamps.removeFirst()
                if (timestamps.size >= 2) {
                    val duration = (timestamps.last() - timestamps.first()).toDouble() / 1000.0
                    updateSampleRate?.invoke((timestamps.size / duration).toInt())
                }
                if (fullTimestamps.size >= 2) {
                    val fullDuration = (fullTimestamps.last() - fullTimestamps.first()).toDouble() / 1000.0
                    updateFullSampleRate?.invoke((fullTimestamps.size / fullDuration).toInt())
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                timestamps.clear()
                fullTimestamps.clear()
            }
        }
        return true
    }
}