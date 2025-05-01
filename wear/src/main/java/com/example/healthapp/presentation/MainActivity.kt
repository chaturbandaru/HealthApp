package com.example.healthapp.presentation

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.*
import com.example.healthapp.R
import com.example.healthapp.presentation.theme.HealthAppTheme
import android.content.pm.PackageManager
import androidx.compose.ui.res.stringResource
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), SensorEventListener, OnMessageReceivedListener {
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private val _heartRate = mutableStateOf(0)
    private val _systolic = mutableStateOf(0)
    private val _diastolic = mutableStateOf(0)
    private val _isMeasuring = mutableStateOf(false)
    private val _sensorAvailable = mutableStateOf(true)
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var messageClient: MessageClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startMonitoring()
        } else {
            // Handle permission denied
            _isMeasuring.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "HealthApp::WakeLock"
        ).apply {
            setReferenceCounted(false)
        }

        // Initialize sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        // Check if heart rate sensor is available
        _sensorAvailable.value = heartRateSensor != null

        // Initialize Wearable API
        messageClient = Wearable.getMessageClient(this)

        setContent {
            HealthAppTheme {
                BPScreen(
                    hr = _heartRate.value,
                    systolic = _systolic.value,
                    diastolic = _diastolic.value,
                    isMeasuring = _isMeasuring.value,
                    sensorAvailable = _sensorAvailable.value,
                    onStartMeasurement = { startMeasurement() },
                    onSendToPhone = { sendBPToPhone() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!wakeLock.isHeld) {
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        }
        messageClient.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        stopMonitoring()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        messageClient.removeListener(this)
    }

    private fun startMeasurement() {
        if (!_sensorAvailable.value) {
            return
        }
        checkPermissionAndStart()
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED -> {
                startMonitoring()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
            }
        }
    }

    private fun startMonitoring() {
        _isMeasuring.value = true
        _heartRate.value = 0
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        _isMeasuring.value = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_HEART_RATE && it.values.isNotEmpty()) {
                val hr = it.values[0].toInt()
                if (hr > 0) {
                    _heartRate.value = hr
                    estimateBP(hr)
                    _isMeasuring.value = false

                    // Automatically send to phone after measurement
                    sendBPToPhone()
                }
            }
        }
    }


    private fun estimateBP(hr: Int) {
        // Simple estimation algorithm (adjust these values based on your requirements)
        _systolic.value = (110 + (hr - 60) * 0.5).roundToInt().coerceIn(80, 190)
        _diastolic.value = (70 + (hr - 60) * 0.3).roundToInt().coerceIn(50, 130)
    }

    private fun sendBPToPhone() {
        try {
            val message = "${_systolic.value}/${_diastolic.value}"
            Wearable.getMessageClient(this).sendMessage(
                "health_app_node", // Node ID of the phone
                "/bp_data", // Message path
                message.toByteArray()
            ).addOnSuccessListener {
                Log.d("MainActivity", "Message sent successfully")
            }.addOnFailureListener {
                Log.e("MainActivity", "Failed to send message", it)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error sending message", e)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/start_measurement") {
            startMeasurement() // Already implemented method to start monitoring
        }
    }

}

@Composable
fun BPScreen(
    hr: Int,
    systolic: Int,
    diastolic: Int,
    isMeasuring: Boolean,
    sensorAvailable: Boolean,
    onStartMeasurement: () -> Unit,
    onSendToPhone: () -> Unit
) {
    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!sensorAvailable) {
                Text(
                    text = stringResource(R.string.no_sensor),
                    style = MaterialTheme.typography.body1,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            } else if (isMeasuring) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.measuring),
                        style = MaterialTheme.typography.body1
                    )
                }
            } else {
                // Blood Pressure Display
                Text(
                    text = stringResource(R.string.bp_format, systolic, diastolic),
                    style = MaterialTheme.typography.display1,
                    color = Color.White
                )


                // Start Measurement Button
                Button(
                    onClick = onStartMeasurement,
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth()
                ) {
                    Text(stringResource(R.string.measure))
                }


                // Disclaimer
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.bp_disclaimer),
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}