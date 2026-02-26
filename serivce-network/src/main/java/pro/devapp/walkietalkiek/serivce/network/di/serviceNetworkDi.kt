package pro.devapp.walkietalkiek.serivce.network.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import pro.devapp.walkietalkiek.serivce.network.ChanelControllerImpl
import pro.devapp.walkietalkiek.serivce.network.ClientController
import pro.devapp.walkietalkiek.serivce.network.ClientInfoResolver
import pro.devapp.walkietalkiek.serivce.network.FloorArbitrationState
import pro.devapp.walkietalkiek.serivce.network.FloorLeaseController
import pro.devapp.walkietalkiek.serivce.network.MessageController
import pro.devapp.walkietalkiek.serivce.network.SocketClient
import pro.devapp.walkietalkiek.serivce.network.SocketServer
import pro.devapp.walkietalkiek.serivce.network.data.ClusterMembershipRepository
import pro.devapp.walkietalkiek.serivce.network.data.ConnectedDevicesRepository
import pro.devapp.walkietalkiek.serivce.network.data.DeviceInfoRepository
import pro.devapp.walkietalkiek.serivce.network.data.PttFloorRepository
import pro.devapp.walkietalkiek.serivce.network.data.TextMessagesRepository

fun Module.registerServiceNetworkDi() {
    factoryOf(::ClientInfoResolver)
    factoryOf(::DeviceInfoRepository)
    singleOf(::ConnectedDevicesRepository)
    singleOf(::ClusterMembershipRepository)
    singleOf(::PttFloorRepository)
    singleOf(::FloorArbitrationState)

    singleOf(::SocketClient)
    singleOf(::SocketServer)
    singleOf(::ChanelControllerImpl).binds(
        arrayOf(
            MessageController::class,
            ClientController::class,
            FloorLeaseController::class
        )
    )
    singleOf(::TextMessagesRepository)
}
