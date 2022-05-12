package com.jansir.androidasmplugin.autotrack

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogEvent(val value: String)

