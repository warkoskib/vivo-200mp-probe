package com.example.vivocamera2probe

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this)

        text.text = """
            VIVO CAMERA2 PROBE
            
            APP IS RUNNING
            
            MainActivity loaded successfully.
        """.trimIndent()

        text.textSize = 22f
        text.setPadding(50, 100, 50, 50)

        setContentView(text)
    }
}
