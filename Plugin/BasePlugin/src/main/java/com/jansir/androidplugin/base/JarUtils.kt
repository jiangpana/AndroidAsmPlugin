package com.jansir.androidplugin.base

import com.jansir.androidplugin.base.transform.TransformInvocationHelper
import com.jansir.androidplugin.base.ext.printThis
import com.jansir.androidplugin.base.utils.DigestUtils
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

internal object JarUtils {

    val needDeleteClasses = arrayListOf(
        "com/sq/mobile/commonpluhostandsqsdk/SQSdkInterface.class",
        "com/sq/mobile/commonpluhostandsqsdk/BasePluginInterface.class"
    );
    fun modifyJarFile(jarFile: File, tempDir: File?, transform: TransformInvocationHelper): File {
        /** 设置输出到的jar  */
        val hexName = DigestUtils.md5Hex(jarFile.absolutePath).substring(0, 8)
        val optJar = File(tempDir, hexName + jarFile.name)
        val jarOutputStream = JarOutputStream(FileOutputStream(optJar))
        jarOutputStream.use {
            val file = JarFile(jarFile)
            val enumeration = file.entries()
            enumeration.iterator().forEach { jarEntry ->
                val inputStream = file.getInputStream(jarEntry)
                val entryName = jarEntry.name
                if (entryName.contains("module-info.class") && !entryName.contains("META-INF")) {
                    printThis("jar file module-info:$entryName jarFileName:${jarFile.path}")
                } else {
                    printThis("entryName =$entryName")
                    if (!needDeleteClasses.contains(entryName)){
                        val zipEntry = ZipEntry(entryName)
                        jarOutputStream.putNextEntry(zipEntry)
                        var modifiedClassBytes: ByteArray? = null
                        val sourceClassBytes = inputStream.readBytes()
                        if (entryName.endsWith(".class")) {
                            try {
                                modifiedClassBytes = transform.process(entryName, sourceClassBytes)
                            } catch (ignored: Exception) {
                            }
                        }
                        /**
                         * 读取原jar
                         */
                        if (modifiedClassBytes == null) {
                            jarOutputStream.write(sourceClassBytes)
                        } else {
                            jarOutputStream.write(modifiedClassBytes)
                        }
                        jarOutputStream.closeEntry()
                    }

                }
            }
        }
        return optJar
    }

    fun scanJarFile(jarFile: File): HashSet<String> {
        val hashSet = HashSet<String>()
        val file = JarFile(jarFile)
        file.use {
            val enumeration = file.entries()
            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement()
                val entryName = jarEntry.name
                if (entryName.contains("module-info.class")) {
                    printThis("module-info:$entryName")
                    continue
                }
                if (entryName.endsWith(".class")) {
                    hashSet.add(entryName)
                }
            }
        }
        return hashSet
    }

    fun deleteJarScan(jarFile: File?, removeClasses: List<String>, callBack: DeleteCallBack?) {
        /**
         * 读取原jar
         */
        val file = JarFile(jarFile)
        file.use {
            val enumeration = file.entries()
            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement()
                val entryName = jarEntry.name
                if (entryName.endsWith(".class") && removeClasses.contains(entryName)) {
                    val inputStream = file.getInputStream(jarEntry)
                    val sourceClassBytes = inputStream.readBytes()
                    try {
                        callBack?.delete(entryName, sourceClassBytes)
                    } catch (ignored: Exception) {
                    }
                }
            }
        }
    }

    fun deleteJarScan(jarFile: File?, callBack: DeleteCallBack?) {
        /**
         * 读取原jar
         */
        val file = JarFile(jarFile)
        file.use {
            val enumeration = file.entries()
            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement()
                val inputStream = file.getInputStream(jarEntry)
                val entryName = jarEntry.name
                val sourceClassBytes = inputStream.readBytes()
                if (entryName.endsWith(".class")) {
                    try {
                        callBack?.delete(entryName, sourceClassBytes)
                    } catch (ignored: Exception) {
                    }
                }
            }
        }
    }
}