package io.delightlabs.xplaandroid.grpc

import cosmos.base.v1beta1.CoinOuterClass
import io.delightlabs.xplaandroid.GRPCClient
import io.delightlabs.xplaandroid.LCDClient
import io.delightlabs.xplaandroid.api.XplaNetwork
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GrpcTest {
    private val lcd = LCDClient(
        network = XplaNetwork.Testnet,
        gasAdjustment = "3",
        gasPrices = listOf(CoinOuterClass.Coin.newBuilder().apply {
            this.amount = "850000000000"
            this.denom = "axpla"
        }.build())
    )
    @Test
    fun authinfo() {
        runBlocking {
            val client = GRPCClient(XplaNetwork.Mainnet)
            val res = client.auth.accountInfo("xpla1tple283l3vcaac7x0mqrwtvm3kfe8hlcmsk4tz")
            assertEquals(5, res.info.accountNumber)
        }
    }

    @Test
    fun grpcWasm() {
        val res = lcd.grpcClient.wasm.contractQueryTokenInfo(
            "xpla1r57m20afwdhkwy67520p8vzdchzecesmlmc8k8w2z7t3h9aevjvs35x4r5"
        )
        assertEquals("CTXT", res?.data?.symbol)
    }

    @Test
    fun testContractQuery() {
        data class TokenInfo(
            val name: String,
            val symbol: String,
            val decimals: Int,
            val total_supply: String,
        )

        val tokenInfo =  lcd.grpcClient.wasm.contractQuery<TokenInfo>(
            "xpla1r57m20afwdhkwy67520p8vzdchzecesmlmc8k8w2z7t3h9aevjvs35x4r5",
            "{\"token_info\":{}}"
        )

        assertEquals("CTXT", tokenInfo?.symbol)
    }

}


