// Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.coroutines

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.CancellationException

/**
 * Coroutine scope that is tied to the full Application closing, or plugin unloading. Default to dispatching to background thread pool.
 *
 * Use this if the coroutine needs to live past a project being closed or across projects such as an Application Service
 */
fun applicationCoroutineScope(coroutineName: String): CoroutineScope =
    PluginCoroutineScopeTracker.getInstance().applicationThreadPoolScope(coroutineName)

/**
 * Coroutine scope that is tied to a project closing, or plugin unloading. Default to dispatching to background thread pool.
 *
 * Use this if the coroutine needs to live past a UI being closed, or tied to a project's life cycle such as a Project Service.
 */
fun projectCoroutineScope(project: Project, coroutineName: String): CoroutineScope =
    PluginCoroutineScopeTracker.getInstance(project).applicationThreadPoolScope(coroutineName)

/**
 * Coroutine scope that is tied to a disposable or a plugin unloading. Default to dispatching to background thread pool.
 *
 * Use this if the coroutine is tied to a UI such as a tool window, or a run configuration
 *
 * **Note: If a call lives past the closing of a UI such as kicking off a resource creation, use [projectCoroutineScope].
 * Otherwise, the coroutine will be canceled when the UI is closed!**
 */
fun disposableCoroutineScope(disposable: Disposable, coroutineName: String): CoroutineScope {
    check(disposable !is Project && disposable !is Application) { "disposable should not be a project or application" }
    return PluginCoroutineScopeTracker.getInstance().applicationThreadPoolScope(coroutineName).also {
        Disposer.register(disposable) {
            it.cancel(CancellationException("Parent disposable was disposed"))
        }
    }
}

/**
 * Version of [applicationCoroutineScope] the class name as the coroutine name.
 */
inline fun <reified T : Any> T.applicationCoroutineScope(): CoroutineScope =
    applicationCoroutineScope(T::class.java.name)

/**
 * Version of [projectCoroutineScope] the class name as the coroutine name.
 */
inline fun <reified T : Any> T.projectCoroutineScope(project: Project): CoroutineScope =
    projectCoroutineScope(project, T::class.java.name)

/**
 * Version of [disposableCoroutineScope] the class name as the coroutine name.
 */
inline fun <reified T : Any> T.disposableCoroutineScope(disposable: Disposable): CoroutineScope =
    disposableCoroutineScope(disposable, T::class.java.name)

class PluginCoroutineScopeTracker : Disposable {
    @PublishedApi
    internal fun applicationThreadPoolScope(coroutineName: String): CoroutineScope = BackgroundThreadPoolScope(coroutineName, this)

    override fun dispose() {}

    companion object {
        fun getInstance() = service<PluginCoroutineScopeTracker>()
        fun getInstance(project: Project) = project.service<PluginCoroutineScopeTracker>()
    }
}

private class BackgroundThreadPoolScope(coroutineName: String, disposable: Disposable) : CoroutineScope {
    override val coroutineContext = SupervisorJob() + CoroutineName(coroutineName) + getCoroutineBgContext()

    init {
        Disposer.register(disposable) {
            coroutineContext.cancel(CancellationException("Parent disposable was disposed"))
        }
    }
}
