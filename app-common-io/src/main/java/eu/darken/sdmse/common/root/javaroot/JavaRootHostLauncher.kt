package eu.darken.sdmse.common.root.javaroot

import eu.darken.sdmse.common.BuildConfigWrap
import eu.darken.sdmse.common.debug.BBDebug
import eu.darken.sdmse.common.debug.logging.Logging.Priority.ERROR
import eu.darken.sdmse.common.debug.logging.asLog
import eu.darken.sdmse.common.debug.logging.log
import eu.darken.sdmse.common.debug.logging.logTag
import eu.darken.sdmse.common.files.core.local.root.FileOpsClient
import eu.darken.sdmse.common.pkgs.pkgops.root.PkgOpsClient
import eu.darken.sdmse.common.root.javaroot.internal.RootHostLauncher
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class JavaRootHostLauncher @Inject constructor(
    private val rootHostLauncher: RootHostLauncher,
    private val fileOpsClientFactory: FileOpsClient.Factory,
    private val pkgOpsClientFactory: PkgOpsClient.Factory,
    private val bbDebug: BBDebug,
) {

    fun create(): Flow<JavaRootClient.Connection> = rootHostLauncher
        .createConnection(
            binderClass = JavaRootConnection::class,
            rootHostClass = JavaRootHost::class,
            enableDebug = BuildConfigWrap.BUILD_TYPE == BuildConfigWrap.BuildType.BETA || bbDebug.isDebug()
        )
        .onStart { log(TAG) { "Initiating connection to host." } }
        .map { ipc ->
            JavaRootClient.Connection(
                ipc = ipc,
                clientModules = listOf(
                    fileOpsClientFactory.create(ipc.fileOps),
                    pkgOpsClientFactory.create(ipc.pkgOps)
                )
            )
        }
        .onEach { log(TAG) { "Connection available: $it" } }
        .catch {
            log(TAG, ERROR) { "Failed to establish connection: ${it.asLog()}" }
            throw RootUnavailableException("Failed to establish connection", cause = it)
        }
        .onCompletion { log(TAG) { "Connection unavailable." } }

    companion object {
        private val TAG = logTag("Root", "Java", "Host", "Launcher")
    }
}