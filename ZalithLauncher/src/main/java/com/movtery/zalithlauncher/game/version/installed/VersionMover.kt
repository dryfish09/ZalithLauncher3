package com.movtery.zalithlauncher.game.version.installed

import android.content.Context
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.TaskFlowExecutor
import com.movtery.zalithlauncher.coroutine.TitledTask
import com.movtery.zalithlauncher.coroutine.addTask
import com.movtery.zalithlauncher.coroutine.buildPhase
import com.movtery.zalithlauncher.game.path.getVersionsHome
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

private const val TAG = "VersionMover"

class VersionMover(
    private val context: Context,
    scope: CoroutineScope,
    private val versions: List<Version>,
    private val targetGamePath: String
) {
    private val taskExecutor = TaskFlowExecutor(scope)
    val tasksFlow: StateFlow<List<TitledTask>> = taskExecutor.tasksFlow

    private val sourceVersionsHome: String = getVersionsHome()
    private val targetVersionsHome: String = File(targetGamePath, "versions").absolutePath

    private val movedVersions = mutableListOf<String>()
    private val failedVersions = mutableListOf<Pair<String, String>>()

    fun start(
        onEnd: (moved: List<String>, failed: List<Pair<String, String>>) -> Unit,
        onThrowable: (Throwable) -> Unit
    ) {
        taskExecutor.executePhasesAsync(
            onStart = {
                movedVersions.clear()
                failedVersions.clear()
                val phases = getTaskPhases()
                taskExecutor.addPhases(phases)
            },
            onComplete = {
                onEnd(movedVersions.toList(), failedVersions.toList())
            },
            onError = onThrowable
        )
    }

    fun cancel() {
        taskExecutor.cancel()
    }

    fun isRunning(): Boolean = taskExecutor.isRunning()

    private suspend fun getTaskPhases() = withContext(Dispatchers.IO) {
        listOf(
            buildPhase {
                addTask(
                    id = "VersionMover.MoveVersions",
                    title = context.getString(R.string.versions_manage_move_versions_progress),
                    icon = R.drawable.ic_autorenew,
                    dispatcher = Dispatchers.IO
                ) { task ->
                    task.updateProgress(-1f)
                    val total = versions.size

                    versions.forEachIndexed { index, version ->
                        ensureActive()
                        val name = version.getVersionName()
                        task.updateMessage(R.string.versions_manage_move_versions_moving, name)

                        val sourceDir = version.getVersionPath()
                        val targetDir = File(targetVersionsHome, name)

                        try {
                            runCatching {
                                if (targetDir.exists()) {
                                    FileUtils.deleteQuietly(targetDir)
                                }
                                targetDir.mkdirs()
                                FileUtils.copyDirectory(sourceDir, targetDir)
                                FileUtils.deleteQuietly(sourceDir)

                                version.getVersionConfig().apply {
                                    setVersionPath(targetDir)
                                    saveWithThrowable()
                                }

                                movedVersions.add(name)
                            }.onFailure { e ->
                                Logger.error(TAG, "Failed to move version $name", e)
                                failedVersions.add(name to (e.message ?: e.javaClass.simpleName))
                                if (targetDir.exists()) {
                                    FileUtils.deleteQuietly(targetDir)
                                }
                            }
                        } catch (e: Exception) {
                            ensureActive()
                            throw e
                        }

                        task.updateProgress(
                            percentage = (index + 1).toFloat() / total.toFloat()
                        )
                    }

                    task.updateProgress(-1f)
                }
            }
        )
    }
}
