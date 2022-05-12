package com.jansir.androidplugin.base

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.IOException

abstract class AsmHelper {
    @Throws(IOException::class)
    fun modifyClass(srcClass: ByteArray?): ByteArray? {
        val classNode = ClassNode(Opcodes.ASM5)
        val classReader = ClassReader(srcClass)
        //1 将读入的字节转为classNode
        classReader.accept(classNode, 0)
        processClassNode(classNode)
        val classWriter = ClassWriter(0)
        //3  将classNode转为字节数组
        classNode.accept(classWriter)
        return classWriter.toByteArray()
    }

    abstract fun processClassNode(classNode: ClassNode)
}