package com.jansir.androidasmplugin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        test1()
        val image = findViewById<ImageView>(R.id.iv_1)
        println("image = ${image.javaClass.name}")
        image.setImageResource(R.drawable.ic_launcher_background)
    }

    private fun test1() {
        TimeStatistical.start()
        println("MainActivity test1")
    }

    private fun test2() {
        Thread.sleep(1000)
        println("MainActivity test2")
    }


}