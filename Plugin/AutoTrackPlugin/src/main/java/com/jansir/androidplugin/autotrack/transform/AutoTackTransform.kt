package com.jansir.androidplugin.autotrack.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.jansir.androidplugin.autotrack.JarUtils
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class AutoTackTransform : Transform() {
    private val TAG = "AutoTackTransform"
    override fun getName() = "AutoTackTransform"
    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun isIncremental() = false

    @Throws(TransformException::class, InterruptedException::class, IOException::class)
    override fun transform(transformInvocation: TransformInvocation) {
        transformInvocation.outputProvider?.deleteAll()
        transformInvocation.inputs.forEach { it ->
            it.directoryInputs.forEach {
                val src = it.file
                val dest = transformInvocation.outputProvider.getContentLocation(
                    it.name,
                    it.contentTypes,
                    it.scopes,
                    Format.DIRECTORY
                )
                logInfo("directoryInputs,dest = " + dest.absolutePath)
                FileUtils.copyDirectory(src, dest)
                src.walkTopDown().filter { it.isFile }
                    .forEach { classFile ->
                        logInfo("directoryInputs ,classFile =${classFile.name} ")
                        if (classFile.name == "MainActivity.class") {
                            //得到 包名/MainActivity.class
                            val classFileAbsolutePath = classFile.absolutePath.replace(
                                src.absolutePath + File.separator, ""
                            )
                            val desFile = File(dest.absolutePath, classFileAbsolutePath)
                            desFile.delete()

                            val srcBytes = IOUtils.toByteArray(FileInputStream(classFile))
                            val classNode = ClassNode(Opcodes.ASM5)
                            val classReader = ClassReader(srcBytes)
                            classReader.accept(classNode, 0)
                            //修改指令
                            classNode.methods.forEach { method ->
                                if (method.name == "onCreate" && method.desc == "(Landroid/os/Bundle;)V") {
                                    method.instructions.forEach {
                                        if (it.opcode == Opcodes.INVOKESPECIAL && it is MethodInsnNode && it.name == "test1") {
                                            it.name = "test2"
                                        }
                                        if (it.opcode == Opcodes.INVOKEVIRTUAL && it is MethodInsnNode
                                            && it.owner == "com/jansir/androidasmplugin/MainActivity"
                                            && it.name == "setContentView"
                                        ) {
                                            method.instructions.insertBefore(
                                                it,
                                                FieldInsnNode(
                                                    Opcodes.GETSTATIC,
                                                    "com/jansir/androidasmplugin/TimeStatistical",
                                                    "INSTANCE",
                                                    "Lcom/jansir/androidasmplugin/TimeStatistical;"
                                                )
                                            )
                                            method.instructions.insertBefore(
                                                it,
                                                MethodInsnNode(
                                                    Opcodes.INVOKEVIRTUAL,
                                                    "com/jansir/androidasmplugin/TimeStatistical",
                                                    "start",
                                                    "()V",
                                                    false
                                                )
                                            )
                                        }
                                    }
                                }
                                if (method.name == "test2") {
                                    method.instructions.forEach {
                                        //代码截止点插入
                                        if ((it.opcode >= Opcodes.IRETURN && it.opcode <= Opcodes.RETURN) || it.opcode == Opcodes.ATHROW) {
                                            method.instructions.insertBefore(
                                                it,
                                                FieldInsnNode(
                                                    Opcodes.GETSTATIC,
                                                    "com/jansir/androidasmplugin/TimeStatistical",
                                                    "INSTANCE",
                                                    "Lcom/jansir/androidasmplugin/TimeStatistical;"
                                                )
                                            )
                                            method.instructions.insertBefore(
                                                it,
                                                MethodInsnNode(
                                                    Opcodes.INVOKEVIRTUAL,
                                                    "com/jansir/androidasmplugin/TimeStatistical",
                                                    "end",
                                                    "()V",
                                                    false
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            val classWriter = ClassWriter(0)
                            classNode.accept(classWriter)
                            val desBytes = classWriter.toByteArray()
                            desFile.createNewFile()
                            FileOutputStream(desFile).write(desBytes)

                        }

                    }
            }

            it.jarInputs.forEach { jarInput ->

                val src = jarInput.file
                logInfo("jarInputs , " + src.name)
                /**
                 * 能否用it.name 替代it.file.name
                 * 这里没直接用name，加了些花里胡哨的功能应该是为了防止同名jar包放到一个文件夹中发生覆盖的情况
                 */
                /*获得输出文件*/
                var destName = jarInput.file.name
                /* 重名名输出文件,因为可能同名,会覆盖*/
                val hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length - 4)
                }
                val dest = transformInvocation.outputProvider!!.getContentLocation(
                    destName + "_" + hexName,
                    jarInput.contentTypes, jarInput.scopes, Format.JAR
                )
                val modifiedJar = JarUtils.modifyJarFile(
                    jarInput.file, transformInvocation.context?.temporaryDir
                ) { name, bytes ->

                    val classNode = ClassNode(Opcodes.ASM5)
                    val classReader = ClassReader(bytes)
                    classReader.accept(classNode, 0)

                    if (classNode.name =="androidx/appcompat/widget/AppCompatImageView") {
                        classNode.superName ="com/jansir/androidasmplugin/HookImageView"
                        println("TAG = classNode.name =${classNode.name}")
                        println("TAG = classNode.signature =${classNode.signature}")
                        println("TAG = classNode.superName =${classNode.superName}")

                    }

                    val classWriter = ClassWriter(0)
                    classNode.accept(classWriter)
                    classWriter.toByteArray()
                }
                copyIfLegal(modifiedJar, dest)

            }
        }
    }

    fun copyIfLegal(srcFile: File?, destFile: File) {
        if (srcFile?.name?.contains("module-info") != true) {
            try {
                srcFile?.apply {
                    FileUtils.copyFile(srcFile, destFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            println("copyIfLegal module-info:" + srcFile.name)
        }
    }


    private fun logInfo(msg: String) {
        println("$TAG - > $msg")
    }
}