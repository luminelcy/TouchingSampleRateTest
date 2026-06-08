package com.example.touchingsampleratetest

import android.os.Bundle
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import java.util.LinkedList

class MainActivity : ComponentActivity() {
    private val timestamps = LinkedList<Long>()
    private val MAX_SAMPLES = 60
    private var updateSampleRate: ((Int) -> Unit)? = null
    private var updateDeviceInfo: ((String, Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var sampleRate by remember { mutableStateOf(0) }
            var deviceName by remember { mutableStateOf("未检测到设备") }
            var deviceId by remember { mutableStateOf(-1) }

            updateSampleRate = { sampleRate = it }
            updateDeviceInfo = { name, id ->
                deviceName = name
                deviceId = id
            }

            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF000000)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${sampleRate} Hz\n${deviceName}\nDevice ID: ${deviceId}",
                    color = Color(0xFFFFFFFF),
                    fontSize = 48.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                timestamps.clear()
                timestamps.addLast(System.currentTimeMillis())
                val device = InputDevice.getDevice(event.deviceId)
                updateDeviceInfo?.invoke(device?.name ?: "未知设备", event.deviceId)
            }
            MotionEvent.ACTION_MOVE -> {
                timestamps.addLast(System.currentTimeMillis())
                while (timestamps.size > MAX_SAMPLES) timestamps.removeFirst()
                if (timestamps.size >= 2) {
                    val duration = (timestamps.last() - timestamps.first()).toDouble() / 1000.0
                    updateSampleRate?.invoke((timestamps.size / duration).toInt())
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                timestamps.clear()
            }
        }
        return true
    }
}