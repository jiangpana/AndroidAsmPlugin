package com.jansir.androidasmplugin

object TimeStatistical {
    private val TAG: String="TimeStatistical"

    private var startTime = 0L

    fun start() {
        startTime = System.currentTimeMillis()
    }


    fun end() {
      val endTime =  System.currentTimeMillis()
      println("$TAG , cost  =${endTime- startTime}")
    }
}