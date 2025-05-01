package com.example.healthapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {
    private lateinit var profileListView: ListView
    private lateinit var createProfileButton: Button
    private lateinit var profiles: MutableList<UserProfile>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        profileListView = findViewById(R.id.profileListView)
        createProfileButton = findViewById(R.id.createProfileButton)
        profiles = loadProfiles().toMutableList()

        refreshProfileList()

        profileListView.setOnItemClickListener { _, _, position, _ ->
            Intent(this, HealthDataActivity::class.java).apply {
                putExtra("profile", profiles[position])
                startActivity(this)
            }
        }

        createProfileButton.setOnClickListener {
            startActivity(Intent(this, ProfileCreationActivity::class.java))
        }

        profileListView.setOnItemLongClickListener { _, _, position, _ ->
            showDeleteConfirmationDialog(position)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        profiles = loadProfiles().toMutableList()
        refreshProfileList()
    }

    private fun refreshProfileList() {
        profileListView.adapter = ArrayAdapter(
            this,
            R.layout.list_item_profile,
            android.R.id.text1,
            profiles.map { it.name }
        )
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Profile")
            .setMessage("Delete ${profiles[position].name}?")
            .setPositiveButton("Delete") { _, _ -> deleteProfile(position) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteProfile(position: Int) {
        profiles.removeAt(position)
        saveProfiles()
        refreshProfileList()
    }

    private fun loadProfiles(): List<UserProfile> {
        val sharedPref = getSharedPreferences("UserProfiles", Context.MODE_PRIVATE)
        return sharedPref.all.values.map {
            Gson().fromJson(it as String, UserProfile::class.java)
        }
    }

    private fun saveProfiles() {
        getSharedPreferences("UserProfiles", Context.MODE_PRIVATE).edit().apply {
            clear()
            profiles.forEach {
                putString(it.name, Gson().toJson(it))
            }
            apply()
        }
    }
}