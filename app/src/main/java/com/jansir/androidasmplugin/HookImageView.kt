package com.jansir.androidasmplugin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView

@SuppressLint("AppCompatCustomView")

abstract class HookImageView@JvmOverloads constructor(context: Context, attrset: AttributeSet? = null, attr: Int = 0) :
    ImageView(context, attrset, attr) {


    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        println("HookImageView setImageDrawable")

    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        println("HookImageView setImageBitmap")

    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        println("HookImageView setImageResource")

    }
}