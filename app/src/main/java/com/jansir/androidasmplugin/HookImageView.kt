package com.jansir.androidasmplugin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Looper
import android.os.MessageQueue
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import java.util.*

@SuppressLint("AppCompatCustomView")

abstract class HookImageView @JvmOverloads constructor(
    context: Context,
    attrset: AttributeSet? = null,
    attr: Int = 0
) :
    ImageView(context, attrset, attr), MessageQueue.IdleHandler {


    companion object{

        val MAX_ALARM_MULTIPLE =2
        val MAX_ALARM_IMAGE_SIZE =20*1024
        val TAG ="HookImageView"
    }
    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        println("HookImageView setImageDrawable(drawable: Drawable?)")
        printStack()
        addImageLegalMonitor()

    }



    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        println("HookImageView setImageBitmap(bm: Bitmap?)")
        printStack()
        addImageLegalMonitor()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        println("HookImageView setImageResource(resId: Int)")
        printStack()
        addImageLegalMonitor()
    }

    private fun addImageLegalMonitor() {
        Looper.myQueue().removeIdleHandler(this);
        Looper.myQueue().addIdleHandler(this);
    }

    override fun queueIdle(): Boolean {
        val drawable = drawable
        val background = background
        if (drawable != null) {
            checkIsLegal(drawable, "图片")
        }
        try {
            if (background != null) {
                checkIsLegal(background, "背景")
            }
        } catch ( e :Exception) {
            e.printStackTrace()
        }

        return false
    }

    private  fun checkIsLegal(drawable: Drawable, tag: String) {
        val viewWidth = measuredWidth
        val viewHeight = measuredHeight
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        // 大小告警判断
        val imageSize: Int = calculateImageSize(drawable)
        if (imageSize > MAX_ALARM_IMAGE_SIZE) {
            Log.e(TAG, "图片加载不合法，" + tag + "大小 -> " + imageSize)
            dealWarning(drawableWidth, drawableHeight, imageSize, drawable)
        }
        // 宽高告警判断
        if (MAX_ALARM_MULTIPLE * viewWidth < drawableWidth) {
            Log.e(TAG, "图片加载不合法, 控件宽度 -> " + viewWidth + " , " + tag + "宽度 -> " + drawableWidth)
            dealWarning(drawableWidth, drawableHeight, imageSize, drawable)
        }
        if (MAX_ALARM_MULTIPLE * viewHeight < drawableHeight) {
            Log.e(TAG, "图片加载不合法, 控件高度 -> " + viewHeight + " , " + tag + "高度 -> " + drawableHeight)
            dealWarning(drawableWidth, drawableHeight, imageSize, drawable)
        }
    }

    private fun dealWarning(
        drawableWidth: Int,
        drawableHeight: Int,
        imageSize: Int,
        drawable: Drawable
    ) {


    }
    private fun printStack() {
        val stringBuilder = StringBuilder()
        for (stackTraceElement in Thread.currentThread().getStackTrace()) {
            stringBuilder
                .append(stackTraceElement.toString())
                .append("\n")
        }
        Log.e(TAG, " 方法调用 -> ${stringBuilder.toString()}")
    }

    private  fun calculateImageSize(drawable: Drawable): Int {
        if (drawable is BitmapDrawable) {
            val bitmap: Bitmap = (drawable as BitmapDrawable).getBitmap()
            return bitmap.byteCount
        }
        val pixelSize = if (drawable.opacity !== PixelFormat.OPAQUE) 4 else 2
        return pixelSize * drawable.intrinsicWidth * drawable.intrinsicHeight
    }
}