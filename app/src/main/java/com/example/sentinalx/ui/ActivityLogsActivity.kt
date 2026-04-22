package com.example.sentinalx.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sentinalx.R

class ActivityLogsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

       // val recyclerLogs = findViewById<RecyclerView>(R.id.recycler_logs)
        //recyclerLogs.layoutManager = LinearLayoutManager(this)
        //recyclerLogs.adapter = ActivityLogAdapter(emptyList()) // Placeholder empty list
    }
}
