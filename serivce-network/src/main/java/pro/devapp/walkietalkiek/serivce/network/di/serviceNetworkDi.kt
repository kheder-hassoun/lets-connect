package pro.devapp.walkietalkiek.serivce.network.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import pro.devapp.walkietalkiek.serivce.network.ChanelControllerImpl
import pro.devapp.walkietalkiek.serivce.network.ClientController
import pro.devapp.walkietalkiek.serivce.network.ClientInfoResolver
import pro.devapp.walkietalkiek.serivce.network.ControlPlaneController
import pro.devapp.walkietalkiek.serivce.network.MessageController
import pro.devapp.walkietalkiek.serivce.network.MqttControlPlaneController
import pro.devapp.walkietalkiek.serivce.network.SocketClient
import pro.devapp.walkietalkiek.serivce.network.SocketServer
import pro.devapp.walkietalkiek.serivce.network.data.ConnectedDevicesRepository
import pro.devapp.walkietalkiek.serivce.network.data.DeviceInfoRepository
import pro.devapp.walkietalkiek.serivce.network.data.PttFloorRepository
import pro.devapp.walkietalkiek.serivce.network.data.TextMessagesRepository

fun Module.registerServiceNetworkDi() {
    factoryOf(::ClientInfoResolver)
    factoryOf(::DeviceInfoRepository)
    singleOf(::ConnectedDevicesRepository)
    singleOf(::PttFloorRepository)

    singleOf(::SocketClient)
    singleOf(::SocketServer)
    singleOf(::MqttControlPlaneController).binds(
        arrayOf(ControlPlaneController::class)
    )
    singleOf(::ChanelControllerImpl).binds(
        arrayOf(
            MessageController::class,
            ClientController::class
        )
    )
    singleOf(::TextMessagesRepository)
}
