package com.example.healthapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healthapp.data.BPMeasurement
import com.example.healthapp.data.DatabaseModule
import com.example.healthapp.databinding.ActivityHealthDataBinding
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.pow
import java.util.Calendar

class HealthDataActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var binding: ActivityHealthDataBinding
    private lateinit var messageClient: MessageClient
    private lateinit var userId: String
    private val database by lazy { DatabaseModule.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Wearable API
        messageClient = Wearable.getMessageClient(this)

        val profile = intent.getParcelableExtra<UserProfile>("profile")

        if (profile != null) {
            userId = profile.name

            // ✅ Set user profile data in the new UI elements
            binding.userName.text = "Name: ${profile.name}"
            binding.userAge.text = "Age: ${calculateAge(profile.dateOfBirth)}"
            binding.userGender.text = "Gender: ${profile.gender}"
            binding.userSmoking.text = "Smokes: ${if (profile.smokes) "Yes" else "No"}"
            binding.userDrinking.text = "Drinks: ${if (profile.drinks) "Yes" else "No"}"
            binding.userActivity.text = "Active: ${if (profile.hasDailyActivity) "Yes" else "No"}"
            binding.userWeight.text = "Weight: ${profile.weight} kg"
            binding.userHeight.text = "Height: ${profile.height} cm"

            binding.bpTextView.text = "Blood Pressure: --/--"

            binding.testButton.setOnClickListener {
                simulateBPMeasurement(profile.name)
            }

            binding.predictButton.setOnClickListener {
                lifecycleScope.launch {
                    predictCardioRisk(profile)
                }
            }

        } else {
            finish()
        }

    }

    private fun simulateBPMeasurement(userId: String) {
        // Send a message to the first connected wearable
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                val node = nodes.firstOrNull()
                if (node != null) {
                    Wearable.getMessageClient(this).sendMessage(
                        node.id,
                        "/start_measurement",
                        "start".toByteArray()
                    ).addOnSuccessListener {
                        Log.d("HealthDataActivity", "Sent start_measurement to ${node.displayName}")
                    }.addOnFailureListener {
                        Log.e("HealthDataActivity", "Failed to send start_measurement", it)
                    }
                } else {
                    Log.w("HealthDataActivity", "No connected wearable found")
                }
            }
            .addOnFailureListener {
                Log.e("HealthDataActivity", "Failed to get connected nodes", it)
            }
    }

    override fun onMessageReceived(message: MessageEvent) {
        if (message.path == "/bp_data") {
            val data = String(message.data).split("/")
            if (data.size == 2) {
                val systolic = data[0].toIntOrNull()
                val diastolic = data[1].toIntOrNull()

                if (systolic != null && diastolic != null) {
                    lifecycleScope.launch {
                        database.bpDao().insert(
                            BPMeasurement(
                                userId = userId,
                                systolic = systolic,
                                diastolic = diastolic
                            )
                        )
                        runOnUiThread {
                            binding.bpTextView.text = "Blood Pressure: $systolic/$diastolic"
                        }
                    }
                }
            }
        }
    }

    private suspend fun predictCardioRisk(profile: UserProfile) {
        val latestBP = database.bpDao().getLatestForUser(profile.name)

        if (latestBP != null) {
            val bmi = profile.weight.toDouble() / ((profile.height.toDouble() / 100).pow(2))
            val age = calculateAge(profile.dateOfBirth)
            val gender = if (profile.gender == "Male") 1 else 0

            val input = CardioInput(
                age = age,
                gender = gender,
                ap_hi = latestBP.systolic,
                ap_lo = latestBP.diastolic,
                smoke = if (profile.smokes) 1 else 0,
                alco = if (profile.drinks) 1 else 0,
                active = if (profile.hasDailyActivity) 1 else 0,
                BMI = bmi
            )

            RetrofitClient.instance.predict(input).enqueue(object : Callback<PredictionResponse> {
                override fun onResponse(call: Call<PredictionResponse>, response: Response<PredictionResponse>) {
                    if (response.isSuccessful) {
                        val result = response.body()?.prediction
                        val message = if (result == 1) {
                            "⚠️ Cardiovascular risk detected!\n\nPlease consult a doctor for further evaluation."
                        } else {
                            "✅ Your heart is doing great!\n\nContinue your healthy habits and stay active."
                        }

                        AlertDialog.Builder(this@HealthDataActivity)
                            .setTitle("Prediction Result")
                            .setMessage(message)
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        AlertDialog.Builder(this@HealthDataActivity)
                            .setTitle("Prediction Failed")
                            .setMessage("Server responded with code: ${response.code()}")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }

                override fun onFailure(call: Call<PredictionResponse>, t: Throwable) {
                    AlertDialog.Builder(this@HealthDataActivity)
                        .setTitle("Error")
                        .setMessage("Something went wrong:\n${t.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            })
        } else {
            Toast.makeText(this, "No BP data found for prediction", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateAge(dob: String): Int {
        val parts = dob.split("/")
        if (parts.size != 3) return 0
        val day = parts[0].toInt()
        val month = parts[1].toInt()
        val year = parts[2].toInt()

        val today = Calendar.getInstance()
        val birth = Calendar.getInstance()
        birth.set(year, month - 1, day)

        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }

    override fun onResume() {
        super.onResume()
        messageClient.addListener(this)
    }

    override fun onPause() {
        messageClient.removeListener(this)
        super.onPause()
    }
}
