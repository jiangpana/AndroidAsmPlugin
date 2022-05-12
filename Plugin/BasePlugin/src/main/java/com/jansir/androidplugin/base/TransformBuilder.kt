package com.jansir.androidplugin.base

import com.android.build.api.transform.TransformInvocation
import com.jansir.androidplugin.base.transform.TransformInvocationHelper

/**
 *
 *  @Author LiABao
 *  @Since 2021/6/30
 *
 */
class TransformBuilder(val callBack: TransformCallBack) {

    var transformInvocation: TransformInvocation? = null

    var single: Boolean = false

    var filter: ClassNameFilter? = null

    var simpleScan = false

    var deleteCallBack: DeleteCallBack? = null

    fun build(): TransformInvocationHelper {
        val transform = TransformInvocationHelper(transformInvocation, callBack, single)
        transform.also {
            it.filter = filter
        }
        if (simpleScan) {
            transform.openSimpleScan()
        }
        deleteCallBack?.apply {
            transform.setDeleteCallBack(this)
        }
        return transform
    }
}