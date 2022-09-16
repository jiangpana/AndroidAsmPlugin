package com.jansir.androidplugin.hotfix.transform

import com.android.tools.r8.internal.it
import com.jansir.androidplugin.base.AsmHelper
import com.jansir.androidplugin.base.ext.getAnnotationValue
import com.jansir.androidplugin.base.ext.hasAnnotation
import com.jansir.androidplugin.base.ext.isMethodEndOpcode
import com.jansir.androidplugin.base.ext.printThis
import com.jansir.androidplugin.base.transform.BaseTransform
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.tree.*

class HotfixTransform : BaseTransform() {
    val sqWanCoreName = "com/sq/mobile/sqsdk/SqWanCore"
    val sqWanCoreNameImpl = "com/sq/mobile/sqsdk/SqWanCoreImpl"
    override fun getAsmHelper(): AsmHelper {
        return object : AsmHelper() {
            override fun processClassNode(classNode: ClassNode) {
                if (classNode.name == sqWanCoreName) {
                    classNode.name = sqWanCoreNameImpl
                }

            }
        }
    }

    override fun getName() = "HotfixTransform"
}