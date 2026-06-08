package com.example.touchingsampleratetest

import android.os.Bundle
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
    private var onSampleRateUpdate: ((Int) -> Unit)? = null
    private val timestamps = LinkedList<Long>()
    private val MAX_SAMPLES = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var sampleRate by remember { mutableStateOf(0) }
            
            onSampleRateUpdate = { sampleRate = it }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000000)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${sampleRate} Hz",
                    color = Color(0xFFFFFFFF),
                    fontSize = 48.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val currentTime = System.currentTimeMillis()

        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                timestamps.addLast(currentTime)

                while (timestamps.size > MAX_SAMPLES) {
                    timestamps.removeFirst()
                }

                if (timestamps.size >= 2) {
                    val firstTime = timestamps.first()
                    val duration = (currentTime - firstTime).toDouble() / 1000.0
                    val count = timestamps.size - 1
                    val newRate = if (duration > 0) (count / duration).toInt() else 0
                    onSampleRateUpdate?.invoke(newRate)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                timestamps.clear()
            }
        }

        return true
    }
}
