package io.delightlabs.xplaandroid.grpc

import cosmos.auth.v1beta1.QueryGrpcKt.QueryCoroutineStub
import cosmos.auth.v1beta1.QueryOuterClass.QueryAccountInfoRequest
import cosmos.auth.v1beta1.QueryOuterClass.QueryAccountInfoResponse
import io.grpc.ManagedChannel

class AuthGRPC(val channel: ManagedChannel) {
    private val stub: QueryCoroutineStub = QueryCoroutineStub(channel)

    suspend fun accountInfo(address: String): QueryAccountInfoResponse {
        val request = QueryAccountInfoRequest.newBuilder()
            .setAddress(address)
            .build()
        return stub.accountInfo(request)
    }
}
