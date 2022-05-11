package com.jansir.androidplugin.autotrack

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.jansir.androidplugin.autotrack.transform.AutoTackTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

class AutoTrackPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val isApp = project.plugins.hasPlugin(AppPlugin::class.java)
        if (isApp) {
            val appExtension = project.extensions.getByType(BaseAppModuleExtension::class.java)
            appExtension.registerTransform(AutoTackTransform())
        }
    }
}