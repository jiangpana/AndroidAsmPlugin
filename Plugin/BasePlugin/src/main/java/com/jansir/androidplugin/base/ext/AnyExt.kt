package com.jansir.androidplugin.base.ext


fun Any.printThis(msg: Any) {
    println(javaClass.simpleName+" -> "+(String.format("{ %s }", msg.toString())))
}