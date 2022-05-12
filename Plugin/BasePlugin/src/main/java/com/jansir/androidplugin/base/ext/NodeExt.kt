package com.jansir.androidplugin.base.ext

import com.android.tools.r8.internal.it
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodNode

fun MethodNode.hasAnnotation(desc: String): Boolean {
    visibleAnnotations?.forEach {
        if (it.desc == desc) {
            return true
        }
    }
    return false
}

fun MethodNode.getAnnotationValue(desc: String): List<Any> {
    try {
        val annos = visibleAnnotations.find { it.desc == desc }
        return annos!!.values
    } catch (e: Exception) {
        return emptyList()
    }
}

fun AbstractInsnNode.isMethodEndOpcode(): Boolean {
    return (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW
}