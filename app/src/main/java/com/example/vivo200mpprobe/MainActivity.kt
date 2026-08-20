package com.example.vivo200mpprobe

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this)
        textView.text = """
            Vivo 200MP Probe
            
            App started successfully.
            
            No camera code is running yet.
        """.trimIndent()

        textView.textSize = 20f
        textView.setPadding(40, 60, 40, 40)

        setContentView(textView)
    }
}
