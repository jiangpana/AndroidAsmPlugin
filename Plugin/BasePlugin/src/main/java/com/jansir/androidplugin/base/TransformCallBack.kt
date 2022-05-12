package com.jansir.androidplugin.base

interface TransformCallBack {
    fun process(className: String, classBytes: ByteArray?): ByteArray?
}