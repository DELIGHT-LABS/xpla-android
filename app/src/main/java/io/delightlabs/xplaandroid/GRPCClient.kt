package io.delightlabs.xplaandroid

import io.delightlabs.xplaandroid.api.XplaNetwork
import io.delightlabs.xplaandroid.grpc.AuthGRPC
import io.grpc.ManagedChannelBuilder
import java.io.Closeable
import java.util.concurrent.TimeUnit

class GRPCClient (
    var network: XplaNetwork,
) : Closeable {
    var channel = ManagedChannelBuilder.forTarget(network.grpc.url).apply {
        if (network.grpc.noTLS) {
            usePlaintext()
        } else {
            useTransportSecurity()
        }
    }.build()

    val auth = AuthGRPC(channel)

    override fun close() {
        channel.shutdown().awaitTermination(10, TimeUnit.SECONDS)
    }
}