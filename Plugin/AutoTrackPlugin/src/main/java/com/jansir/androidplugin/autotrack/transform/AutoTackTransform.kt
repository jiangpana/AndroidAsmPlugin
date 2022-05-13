package com.jansir.androidplugin.autotrack.transform

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

class AutoTackTransform : BaseTransform() {
    val logEventAnnoDes = "Lcom/jansir/androidasmplugin/autotrack/LogEvent;"
    val hookthread = "com/jansir/androidasmplugin/hookthread/HookThread"
    val javaThread = "java/lang/Thread"
    override fun getAsmHelper(): AsmHelper {
        return object : AsmHelper() {
            override fun processClassNode(classNode: ClassNode) {

                //处理内部类情况
                if (classNode.name != hookthread &&
                    classNode.superName == javaThread
                ) {
                    printThis("违规 -> "+classNode.name )
                }
                classNode.methods?.forEach { method ->
                    if (method.hasAnnotation(logEventAnnoDes)) {
                        insertEventLogger(method, method.getAnnotationValue(logEventAnnoDes))
                    }
                    method.instructions.forEach {
                        if (it.opcode == Opcodes.NEW && it is TypeInsnNode && it.desc == javaThread) {
                            printThis("it.desc = ${it.desc} , ${classNode.name}")
                            it.desc = hookthread
                        }
                    }
                }

                if (classNode.name == "androidx/appcompat/widget/AppCompatImageView") {
                    printThis("processClassNode , ${classNode.name}")
                    classNode.superName = "com/jansir/androidasmplugin/HookImageView"
                }

                if (classNode.name == "com/jansir/androidasmplugin/MainActivity") {
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
                                    method.instructions.apply {
                                        insertBefore(
                                            it,
                                            FieldInsnNode(
                                                Opcodes.GETSTATIC,
                                                "com/jansir/androidasmplugin/TimeStatistical",
                                                "INSTANCE",
                                                "Lcom/jansir/androidasmplugin/TimeStatistical;"
                                            )
                                        )
                                        insertBefore(it, LdcInsnNode("测试测试"))
                                        insertBefore(
                                            it,
                                            MethodInsnNode(
                                                Opcodes.INVOKEVIRTUAL,
                                                "com/jansir/androidasmplugin/TimeStatistical",
                                                "start",
                                                "(Ljava/lang/String;)V",
                                                false
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        if (method.name == "test2") {
                            method.instructions.forEach {
                                //代码截止点插入
                                if ((it.opcode >= Opcodes.IRETURN && it.opcode <= Opcodes.RETURN) || it.opcode == Opcodes.ATHROW) {
                                    method.instructions.apply {
                                        insertBefore(
                                            it,
                                            FieldInsnNode(
                                                Opcodes.GETSTATIC,
                                                "com/jansir/androidasmplugin/TimeStatistical",
                                                "INSTANCE",
                                                "Lcom/jansir/androidasmplugin/TimeStatistical;"
                                            )
                                        )
                                        insertBefore(
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
                    }
                }
            }

            private fun insertEventLogger(method: MethodNode?, annotationValue: List<Any>) {
                method!!.instructions?.forEach {
                    if (it.isMethodEndOpcode()) {
                        method.instructions.apply {
                            insertBefore(
                                it, FieldInsnNode(
                                    Opcodes.GETSTATIC,
                                    "com/jansir/androidasmplugin/autotrack/EventLogger", "INSTANCE",
                                    "Lcom/jansir/androidasmplugin/autotrack/EventLogger;"
                                )
                            )

                            insertBefore(it, LdcInsnNode(annotationValue[1]))
                            insertBefore(
                                it,
                                MethodInsnNode(
                                    Opcodes.INVOKEVIRTUAL,
                                    "com/jansir/androidasmplugin/autotrack/EventLogger",
                                    "log",
                                    "(Ljava/lang/String;)V"
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun getName() = "AutoTackTransform"
}