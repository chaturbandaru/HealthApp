package com.example.healthapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.healthapp.databinding.ActivityProfileCreationBinding
import com.google.gson.Gson
import java.util.Calendar

class ProfileCreationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileCreationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityProfileCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up date picker
        binding.dobInput.setOnClickListener { showDatePicker() }

        // Set up submit button
        binding.submitButton.setOnClickListener {
            if (validateInputs()) {
                saveProfile()
                finish()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                binding.dobInput.setText("%02d/%02d/%04d".format(day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun validateInputs(): Boolean {
        if (binding.nameInput.text.isBlank()) {
            showError("Please enter a name")
            return false
        }

        if (binding.dobInput.text.isBlank()) {
            showError("Please select date of birth")
            return false
        }

        if (binding.weightInput.text.toString().toFloatOrNull() == null) {
            showError("Please enter valid weight")
            return false
        }

        if (binding.heightInput.text.toString().toFloatOrNull() == null) {
            showError("Please enter valid height")
            return false
        }

        if (!binding.maleRadioButton.isChecked && !binding.femaleRadioButton.isChecked) {
            showError("Please select gender")
            return false
        }

        return true
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun saveProfile() {
        val gender = when {
            binding.maleRadioButton.isChecked -> "Male"
            binding.femaleRadioButton.isChecked -> "Female"
            else -> "" // Should never reach here due to validation
        }

        val profile = UserProfile(
            name = binding.nameInput.text.toString(),
            dateOfBirth = binding.dobInput.text.toString(),
            weight = binding.weightInput.text.toString().toFloat(),
            height = binding.heightInput.text.toString().toFloat(),
            smokes = binding.smokingCheckbox.isChecked,
            drinks = binding.drinkingCheckbox.isChecked,
            hasDailyActivity = binding.dailyActivityCheckbox.isChecked,
            gender = gender
        )

        getSharedPreferences("UserProfiles", MODE_PRIVATE).edit().apply {
            putString(profile.name, Gson().toJson(profile))
            apply()
        }

        Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
    }
}
