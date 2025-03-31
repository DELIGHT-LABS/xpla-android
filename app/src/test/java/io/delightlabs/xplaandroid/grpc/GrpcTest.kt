package io.delightlabs.xplaandroid.grpc

import io.delightlabs.xplaandroid.GRPCClient
import io.delightlabs.xplaandroid.api.XplaNetwork
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GrpcTest {
    @Test
    fun authinfo() {
        runBlocking {
            val client = GRPCClient(XplaNetwork.Mainnet)
            val res = client.auth.accountInfo("xpla1tple283l3vcaac7x0mqrwtvm3kfe8hlcmsk4tz")
            assertEquals(5, res.info.accountNumber)
        }
    }
}