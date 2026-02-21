package pro.devapp.walkietalkiek.serivce.network

object FloorControlProtocol {
    private const val PREFIX = "__PTT_FLOOR__:"
    private const val TAKEN = "TAKEN"
    private const val RELEASED = "RELEASED"

    fun acquirePacket(): ByteArray = "$PREFIX$TAKEN".toByteArray()

    fun releasePacket(): ByteArray = "$PREFIX$RELEASED".toByteArray()

    fun parse(message: String): FloorControlCommand? {
        return when (message.trim()) {
            "$PREFIX$TAKEN" -> FloorControlCommand.Acquire
            "$PREFIX$RELEASED" -> FloorControlCommand.Release
            else -> null
        }
    }
}

enum class FloorControlCommand {
    Acquire,
    Release
}
