package io.delightlabs.xplaandroid.api

import io.delightlabs.xplaandroid.LCDClient
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

class TxAPI(private val lcdClient: LCDClient): BaseAPI(lcdClient.apiRequester) {

    private val retrofit = lcdClient.apiRequester
    private val tx: Tx = retrofit.getInstance().create(Tx::class.java)
    enum class BroadcastMode{
        BROADCAST_MODE_SYNC,
        BROADCAST_MODE_BLOCK,
        BROADCAST_MODE_ASYNC
    }

    fun createTx(){

    }

    fun estimateFee(

    ){

    }

    fun estimateGas(
    ):Int {
        return 0
    }

}



interface Tx {
    @POST("/cosmos/tx/v1beta1/simulate")
    fun simulate(
        @Body tx_bytes: String
    ): Call<APIReturn.SimulateTx>

    @POST("/cosmos/tx/v1beta1/txs")
    fun broadcast(
        @Body tx_bytes: String,
        @Body mode: String
    ): Call<APIReturn.TxResponse>
}