package com.jansir.androidasmplugin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import com.jansir.androidasmplugin.autotrack.EventLogger
import com.jansir.androidasmplugin.autotrack.LogEvent

class MainActivity : AppCompatActivity() {
    private val TAG="MainActivity"

    @LogEvent("onCreate")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val image = findViewById<ImageView>(R.id.iv_1)
        image.setImageResource(R.mipmap.ic_launcher)
        test1()
    }


    @LogEvent("onStart")
    override fun onStart() {
        super.onStart()

    }

    @LogEvent("onResume")
    override fun onResume() {
        super.onResume()
    }


    private fun test1() {
        println("MainActivity test1")
    }


    @LogEvent("logTest2")
    private fun test2() {
        Thread.sleep(1000)
        val stringBuilder = StringBuilder()
        Thread.currentThread().stackTrace.filterIndexed { index, stackTraceElement ->
            index>1
        }.forEach {
            stringBuilder
                .append(it.toString())
                .append("\n")
        }
        Log.e(TAG, "test2方法调用 :\n${stringBuilder.toString()}")
    }

    fun test3(){
        EventLogger.log("value")
    }

}