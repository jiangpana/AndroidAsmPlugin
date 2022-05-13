package com.jansir.androidasmplugin.hookthread

import java.util.concurrent.Executors

open class HookThread @JvmOverloads constructor(private val runnable:Runnable?=null): Thread(runnable) {

    override fun start() {
        println("HookThread start")
        Executors.newSingleThreadExecutor().execute {
            run()
        }
    }

    override fun run() {
        super.run()
    }
}