package com.specknet.pdiotapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.specknet.pdiotapp.live.LiveDataActivity
import com.specknet.pdiotapp.live.LiveDataActivityBackup
import com.specknet.pdiotapp.live.LiveDataActivityTask2
import com.specknet.pdiotapp.live.LiveDataActivityTask3

class ChoosingTaskActivity : AppCompatActivity() {

    private lateinit var task1: Button
    private lateinit var task2: Button
    private lateinit var task3: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view to your layout file, replace 'R.layout.activity_choosing_task' with your actual layout file
        setContentView(R.layout.activity_choosing_task)

        // Initialize the buttons
        task1 = findViewById(R.id.task1button)
        task2 = findViewById(R.id.task2button)
        task3 = findViewById(R.id.task3button)

        // Set click listeners
        task1.setOnClickListener {
            val intent = Intent(this, LiveDataActivity::class.java)
            startActivity(intent)
        }

        task2.setOnClickListener {
            val intent = Intent(this, LiveDataActivityTask2::class.java)
            startActivity(intent)
        }

        // Add the appropriate action for task3 if required
        task3.setOnClickListener {
            val intent = Intent(this, LiveDataActivityTask3::class.java)
            startActivity(intent)
        }
    }
}
