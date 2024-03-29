package com.jansir.androidplugin.base.transform

import com.android.build.api.transform.*
import com.jansir.androidplugin.base.*
import com.jansir.androidplugin.base.JarUtils
import com.jansir.androidplugin.base.asm.copyIfLegal
import com.jansir.androidplugin.base.ext.printThis
import com.jansir.androidplugin.base.utils.DigestUtils
import com.jansir.androidplugin.base.utils.deleteAll
import com.jansir.androidplugin.base.utils.filterTest
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool

class TransformInvocationHelper(
    transformInvocation: TransformInvocation?,
    callBack: TransformCallBack,
    single: Boolean = false
) {
    private var mCallBack: TransformCallBack? = callBack
    var context: Context? = null
    private var inputs: Collection<TransformInput>? = null
    private var outputProvider: TransformOutputProvider? = null
    private var isIncremental = false
    private var deleteCallBack: DeleteCallBack? = null
    private var simpleScan = false
    var filter: ClassNameFilter? = null
    private val executor: ExecutorService
    private val tasks: MutableList<Callable<Void>> = ArrayList()
    private val destFiles = mutableListOf<File>()

    init {
        context = transformInvocation?.context
        inputs = transformInvocation?.inputs
        outputProvider = transformInvocation?.outputProvider
        isIncremental = transformInvocation?.isIncremental ?: false
        executor = if (!single) {
            ForkJoinPool.commonPool()
        } else {
            Executors.newSingleThreadExecutor()
        }
    }

    fun openSimpleScan() {
        simpleScan = true
    }

    fun setDeleteCallBack(deleteCallBack: DeleteCallBack?) {
        this.deleteCallBack = deleteCallBack
    }

    fun startTransform() {
        try {
            val startTimeUsage = System.currentTimeMillis()
            if (!isIncremental) {
                outputProvider?.deleteAll()
            }
            inputs?.forEach { input ->
                for (jarInput in input.jarInputs) {
                    handleJarInput(jarInput)
                }
                for (directoryInput in input.directoryInputs) {
                    handleDirectoryInput(directoryInput)
                }
            }
            executor.invokeAll(tasks)

            destFiles.forEach {
                it.filterTest("temp")?.forEach { file ->
                    file.deleteAll()
                }
            }
            val timeUsage = System.currentTimeMillis() - startTimeUsage
            printThis("transform coast time: $timeUsage ms")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleJarInput(jarInput: JarInput) {
        val status = jarInput.status
        var destName = jarInput.file.name
        /* 重名名输出文件,因为可能同名,会覆盖*/
        val hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length - 4)
        }
        /*获得输出文件*/
        val dest = outputProvider!!.getContentLocation(
            destName + "_" + hexName,
            jarInput.contentTypes, jarInput.scopes, Format.JAR
        )
        printThis("jarOut path = ${dest.absolutePath}")
        if (isIncremental) {
            when (status) {
                Status.ADDED -> foreachJar(dest, jarInput)
                Status.CHANGED -> diffJar(dest, jarInput)
                Status.REMOVED -> try {
                    deleteScan(dest)
                    if (dest.exists()) {
                        FileUtils.forceDelete(dest)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                else -> {

                }
            }
        } else {
            foreachJar(dest, jarInput)
        }
    }

    private fun diffJar(dest: File, jarInput: JarInput) {
        try {
            val oldJarFileName = JarUtils.scanJarFile(dest)
            val newJarFileName = JarUtils.scanJarFile(jarInput.file)
            val diff = SetDiff(oldJarFileName, newJarFileName)
            val removeList = diff.removedList
            if (removeList.size > 0) {
                JarUtils.deleteJarScan(dest, removeList, deleteCallBack)
            }
            foreachJar(dest, jarInput)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun foreachJar(dest: File, jarInput: JarInput) {
        val task = Callable<Void> {
            try {
                if (!simpleScan) {
                    val modifiedJar =
                        JarUtils.modifyJarFile(jarInput.file, context?.temporaryDir, this)
                    copyIfLegal(modifiedJar, dest)
                } else {
                    val jarFile = jarInput.file
                    val classNames = JarUtils.scanJarFile(jarFile)
                    for (className in classNames) {
                        process(className, null)
                    }
                    copyIfLegal(jarFile, dest)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }
        tasks.add(task)
    }

    @Throws(IOException::class)
    private fun handleDirectoryInput(directoryInput: DirectoryInput) {
        //输出文件目录file
        val outDir = outputProvider!!.getContentLocation(
                directoryInput.name, directoryInput.contentTypes,
                directoryInput.scopes, Format.DIRECTORY
        )
        destFiles.add(outDir)
        //改变的文件集合
        val map = directoryInput.changedFiles
        //输出源文件的目录
        val inDir = directoryInput.file
        printThis("directoryOutput absolutePath =${outDir.absolutePath}")
        if (isIncremental) {
            //处理增量编译情况
            for ((file, status) in map) {
                //file = 已经改变的文件 , 得到改变文件输出file的绝对路径
                val outFilePath = file.absolutePath.replace(inDir.absolutePath, outDir.absolutePath)
                val outFile = File(outFilePath)
                when (status) {
                    Status.ADDED, Status.CHANGED -> {
                        val callable = Callable<Void> {
                            try {
                                FileUtils.touch(outFile)
                            } catch (ignored: Exception) {
                                //  Files.createParentDirs(destFile)
                            }
                            modifySingleFile(inDir, file, outFile)
                            null
                        }
                        tasks.add(callable)
                    }
                    Status.REMOVED -> deleteDirectory(outFile, outDir)
                    else -> {
                    }
                }
            }
        } else {
            changeFile(inDir, outDir)
        }
    }

    private fun deleteDirectory(destFile: File, dest: File) {
        try {
            if (destFile.isDirectory) {
                destFile.walkTopDown().forEach { classFile ->
                    deleteSingle(classFile, dest)
                }
            } else {
                deleteSingle(destFile, dest)
            }
        } catch (ignored: Exception) {
        }
        try {
            if (destFile.exists()) {
                FileUtils.forceDelete(destFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteSingle(classFile: File, dest: File) {
        try {
            if (classFile.name.endsWith(".class")) {
                val absolutePath = classFile.absolutePath.replace(
                        dest.absolutePath +
                                File.separator, ""
                )
                val className = ClassUtils.path2Classname(absolutePath)
                val bytes = IOUtils.toByteArray(FileInputStream(classFile))
                deleteCallBack?.delete(className, bytes)

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun modifySingleFile(dir: File, file: File, dest: File) {
        try {
            val absolutePath = file.absolutePath.replace(
                    dir.absolutePath +
                            File.separator, ""
            )
            val className = ClassUtils.path2Classname(absolutePath)
            if (absolutePath.endsWith(".class")) {
                var modifiedBytes: ByteArray?
                val bytes = IOUtils.toByteArray(FileInputStream(file))
                modifiedBytes = if (!simpleScan) {
                    process(className, bytes)
                } else {
                    process(className, null)
                }
                if (modifiedBytes == null) {
                    modifiedBytes = bytes
                }
                ClassUtils.saveFile(dest, modifiedBytes)
            } else {
                if (!file.isDirectory) {
                    copyIfLegal(file, dest)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun process(className: String, classBytes: ByteArray?): ByteArray? {
        try {
            if (filter == null) {
                filter = DefaultClassNameFilter()
            }
            if (filter?.filter(className) == false) {
                return mCallBack?.process(className, classBytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @Throws(IOException::class)
    private fun changeFile(inDir: File, outDir: File) {
        if (inDir.isDirectory) {
            FileUtils.copyDirectory(inDir, outDir)
            inDir.walkTopDown().filter { it.isFile }
                    .forEach { inClassFile ->
                        if (inClassFile.name.endsWith(".class")) {
                            val task = Callable<Void> {
                                val absolutePath = inClassFile.absolutePath.replace(
                                        inDir.absolutePath + File.separator, ""
                                )
                                val className = ClassUtils.path2Classname(absolutePath)
                                if (!simpleScan) {
                                    val bytes = IOUtils.toByteArray(FileInputStream(inClassFile))
                                    val modifiedBytes = process(className, bytes)
                                    modifiedBytes?.let { saveClassFile(it, outDir, absolutePath) }
                                } else {
                                    process(className, null)
                                }
                                null
                            }
                            tasks.add(task)
                        }
                    }
        }
    }

    @Throws(Exception::class)
    private fun saveClassFile(modifiedBytes: ByteArray, outDir: File, absolutePath: String) {
        val tempDir = File(outDir, "/temp")
        val tempFile = File(tempDir, absolutePath)
        tempFile.mkdirs()
        val modified = ClassUtils.saveFile(tempFile, modifiedBytes)
        //key为相对路径
        val target = File(outDir, absolutePath)
        if (target.exists()) {
            target.delete()
        }
        copyIfLegal(modified, target)
        tempFile.delete()
    }



    private fun deleteScan(dest: File) {
        try {
            JarUtils.deleteJarScan(dest, deleteCallBack)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}