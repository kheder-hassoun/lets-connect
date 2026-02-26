package pro.devapp.walkietalkiek.serivce.network

sealed interface FloorLeaseRequestResult {
    data object Granted : FloorLeaseRequestResult
    data object Pending : FloorLeaseRequestResult
}

interface FloorLeaseController {
    fun requestFloor(): FloorLeaseRequestResult
    fun releaseFloor()
}
