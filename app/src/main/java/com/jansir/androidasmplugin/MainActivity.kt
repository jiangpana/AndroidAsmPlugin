package com.jansir.androidasmplugin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import com.jansir.androidasmplugin.autotrack.EventLogger
import com.jansir.androidasmplugin.autotrack.LogEvent
import com.jansir.androidasmplugin.hookthread.ThreadProxy
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val TAG="MainActivity"

    @LogEvent("onCreate")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
/*        val image = findViewById<ImageView>(R.id.iv_1)
        image.setImageResource(R.mipmap.ic_launcher)
        test1()
        test4()*/
        test5()
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

    fun test4(){
        thread {
            println("MainActivity test4  thread {} , thread name = ${Thread.currentThread().name}")
        }
/*        Thread {
            println("MainActivity test4  Thread(runnable), thread name = ${Thread.currentThread().name} )")
        }.start()

        Thread {
            println("MainActivity test4  Thread2(runnable), thread name = ${Thread.currentThread().name} )")
        }.start()

        Thread {
            println("MainActivity test4  Thread3(runnable), thread name = ${Thread.currentThread().name} )")
        }.start()*/
    }
    fun test5(){
      val thread =  object : Thread() {
            public override fun run() {
                println("run test5")
            }
        }
        thread.start()

    }

}