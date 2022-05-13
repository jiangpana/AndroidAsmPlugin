package com.jansir.androidasmplugin.hookthread

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

object ThreadProxy {

    fun proxy (obj : Runnable):Runnable{
        return Proxy.newProxyInstance(obj.javaClass.classLoader , arrayOf(Runnable::class.java)
        ) { proxy, method, args ->
            method.invoke(obj,args)
        } as Runnable
    }
}