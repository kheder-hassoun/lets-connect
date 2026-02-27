package pro.devapp.walkietalkiek.core.diagnostics

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceLogStore(
    private val context: Context
) {
    private val lock = Any()
    private val timestampFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun appLogFile(): File = File(logsDir(), APP_LOG_FILE_NAME)

    fun crashLogFile(): File = File(logsDir(), CRASH_LOG_FILE_NAME)

    fun logsDirectoryPath(): String = logsDir().absolutePath

    fun clearLogs() {
        synchronized(lock) {
            runCatching {
                listOf(
                    appLogFile(),
                    File(logsDir(), APP_LOG_BACKUP_FILE_NAME),
                    crashLogFile()
                ).forEach { file ->
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }.onFailure { error ->
                Log.e("DeviceLogStore", "Failed to clear logs", error)
            }
        }
    }

    fun append(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        synchronized(lock) {
            runCatching {
                rotateAppLogIfNeeded()
                appLogFile().appendText(
                    buildString {
                        append(timestampFormatter.format(Date()))
                        append(" ")
                        append(priorityToLabel(priority))
                        append("/")
                        append(tag ?: "App")
                        append(": ")
                        append(message)
                        append('\n')
                        if (throwable != null) {
                            val stack = throwable.stackTraceToString()
                            append(stack)
                            if (!stack.endsWith('\n')) {
                                append('\n')
                            }
                        }
                    }
                )
            }.onFailure { error ->
                Log.e("DeviceLogStore", "Failed to append log", error)
            }
        }
    }

    fun writeCrash(threadName: String, throwable: Throwable) {
        synchronized(lock) {
            runCatching {
                val file = crashLogFile()
                file.parentFile?.mkdirs()
                PrintWriter(file).use { writer ->
                    writer.println("thread=$threadName")
                    writer.println("timeMs=${System.currentTimeMillis()}")
                    writer.println()
                    throwable.printStackTrace(writer)
                }
            }.onFailure { error ->
                Log.e("DeviceLogStore", "Failed to write crash log", error)
            }
        }
    }

    private fun logsDir(): File {
        val external = context.getExternalFilesDir("logs")
        val dir = external ?: File(context.filesDir, "logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun rotateAppLogIfNeeded() {
        val current = appLogFile()
        if (!current.exists() || current.length() < MAX_APP_LOG_BYTES) {
            return
        }
        val backup = File(logsDir(), APP_LOG_BACKUP_FILE_NAME)
        if (backup.exists()) {
            backup.delete()
        }
        current.renameTo(backup)
        current.writeText("")
    }
}

private fun priorityToLabel(priority: Int): String = when (priority) {
    Log.VERBOSE -> "V"
    Log.DEBUG -> "D"
    Log.INFO -> "I"
    Log.WARN -> "W"
    Log.ERROR -> "E"
    Log.ASSERT -> "A"
    else -> priority.toString()
}

private const val APP_LOG_FILE_NAME = "app.log"
private const val APP_LOG_BACKUP_FILE_NAME = "app.log.1"
private const val CRASH_LOG_FILE_NAME = "last_crash.txt"
private const val MAX_APP_LOG_BYTES = 1_000_000L
