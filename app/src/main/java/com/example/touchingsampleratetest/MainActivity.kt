package com.example.touchingsampleratetest

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

    /** 由窗口内时间戳计算采样率。N 个点之间只有 N-1 个间隔；跨度为 0 时返回 null。 */
    private fun sampleRateOf(times: List<Long>): Int? {
        if (times.size < 2) return null
        val spanMs = times.last() - times.first()
        if (spanMs <= 0) return null
        return ((times.size - 1) * 1000.0 / spanMs).toInt()
    }

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
                                    trailPoints.clear()
                                    trailPoints.add(down.position)
                                    val now = SystemClock.elapsedRealtime()
                                    // 两个链表用的时钟不同，但各自内部一致，跨度仍然有效
                                    timestamps.clear()
                                    fullTimestamps.clear()
                                    timestamps.addLast(now)
                                    fullTimestamps.addLast(down.uptimeMillis)
                                    drag(down.id) { change ->
                                        trailPoints.add(change.position)
                                        if (trailPoints.size > MAX_SAMPLES) {
                                            trailPoints.removeAt(0)
                                        }
                                        // 批量投递时积攒的中间采样点：时间早于 change 自身，先入队
                                        val historical = change.historical
                                        for (i in historical.indices) {
                                            fullTimestamps.addLast(historical[i].uptimeMillis)
                                        }
                                        val dragNow = SystemClock.elapsedRealtime()
                                        timestamps.addLast(dragNow)
                                        fullTimestamps.addLast(change.uptimeMillis)
                                        while (timestamps.size > MAX_SAMPLES) timestamps.removeFirst()
                                        while (fullTimestamps.size > MAX_SAMPLES) fullTimestamps.removeFirst()
                                        sampleRateOf(timestamps)?.let { updateSampleRate?.invoke(it) }
                                        sampleRateOf(fullTimestamps)?.let { updateFullSampleRate?.invoke(it) }
                                    }
                                    trailPoints.clear()
                                    timestamps.clear()
                                    fullTimestamps.clear()
                                    updateSampleRate?.invoke(0)
                                    updateFullSampleRate?.invoke(0)
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
                            }
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
                                updateSampleRate?.invoke(0)
                                updateFullSampleRate?.invoke(0)
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * 用系统派发的真实事件请求取消批量投递，这是事件进入 App 的最早时机。
     * 别改回用 MotionEvent.obtain 伪造事件 —— 那样能否生效取决于它的默认 source。
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (useUnbuffered && event.actionMasked == MotionEvent.ACTION_DOWN) {
            window.decorView.requestUnbufferedDispatch(event)
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
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
                sampleRateOf(timestamps)?.let { updateSampleRate?.invoke(it) }
                sampleRateOf(fullTimestamps)?.let { updateFullSampleRate?.invoke(it) }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                timestamps.clear()
                fullTimestamps.clear()
                updateSampleRate?.invoke(0)
                updateFullSampleRate?.invoke(0)
            }
        }
        return true
    }
}