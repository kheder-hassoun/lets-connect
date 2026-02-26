package pro.devapp.walkietalkiek.serivce.network

interface ChatPublisher {
    fun publishChatMessage(message: String): Boolean
}
