package taboolib.platform

import de.dytanic.cloudnet.CloudNet
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.service.PlatformExecutor
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * TabooLib
 * taboolib.platform.BungeeExecutor
 *
 * @author CziSKY
 * @since 2021/6/16 0:13
 */
@Awake
@PlatformSide([Platform.CLOUDNET_V3])
class CloudNetV3Executor : PlatformExecutor {

    private val tasks = ArrayList<PlatformExecutor.PlatformRunnable>()
    private val executor = Executors.newScheduledThreadPool(16)

    override fun start() {
    }

    override fun submit(runnable: PlatformExecutor.PlatformRunnable): PlatformExecutor.PlatformTask {
        val future = CompletableFuture<Unit>()
        val task = AppPlatformTask(future)
        val scheduledTask = when {
            runnable.now -> {
                runnable.executor(task)
                null
            }
            runnable.period > 0 -> {
                executor.scheduleAtFixedRate({ runnable.executor(task) }, runnable.delay * 50L, runnable.period * 50L, TimeUnit.MILLISECONDS)
            }
            runnable.delay > 0 -> {
                executor.schedule({ runnable.executor(task) }, runnable.delay * 50L, TimeUnit.MILLISECONDS)
            }
            else -> {
                executor.submit { runnable.executor(task) }
            }
        }
        future.thenAccept {
            scheduledTask?.cancel(false)
        }
        return task
    }

    class AppPlatformTask(private val future: CompletableFuture<Unit>) : PlatformExecutor.PlatformTask {

        override fun cancel() {
            future.complete(null)
        }
    }
}