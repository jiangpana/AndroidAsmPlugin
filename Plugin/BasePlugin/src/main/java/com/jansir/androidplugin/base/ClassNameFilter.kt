package com.jansir.androidplugin.base

interface ClassNameFilter {
    fun filter(className: String): Boolean
}