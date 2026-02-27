package pro.devapp.walkietalkiek.core.diagnostics

import timber.log.Timber

class DeviceFileLogTree(
    private val deviceLogStore: DeviceLogStore
) : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        deviceLogStore.append(
            priority = priority,
            tag = tag,
            message = message,
            throwable = t
        )
    }
}
