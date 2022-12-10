package com.example.spoton_v3

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.RoundingMode
import java.text.DecimalFormat


class ReturnActivity : AppCompatActivity() {
    private var aircraft: String = ""
    private var score: Float = 0.0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_return)

        val bundle: Bundle? = intent.extras

        if (bundle != null) {
            aircraft = bundle.getString("aircraft").toString()
            score = bundle.getFloat("score")
            score *= 100
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.DOWN
            var roundScore = df.format(score).toString()
            roundScore = "${roundScore}%"

            val textView3 = findViewById<TextView>(R.id.textView3)
            textView3.text = aircraft

            val textView4 = findViewById<TextView>(R.id.textView4)
            textView4.text = roundScore
        }

        val btn = findViewById<Button>(R.id.button2)
        btn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}