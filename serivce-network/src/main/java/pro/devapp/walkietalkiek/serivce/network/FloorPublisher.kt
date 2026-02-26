package pro.devapp.walkietalkiek.serivce.network

interface FloorPublisher {
    fun publishAcquire(): Boolean
    fun publishRelease(): Boolean
}
