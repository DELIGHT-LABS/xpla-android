package io.delightlabs.xplaandroid.api

import android.util.Base64
import android.util.Log
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import java.lang.Exception


class WasmAPI(private val retrofit: RetrofitConnection): BaseAPI(retrofit) {

    private val wasm = retrofit.getInstance().create(Wasm::class.java)
    fun contractQuery(
        contractAddr: String,
        queryJson: String
    ): APIReturn.SmartQuery? {
        val encodedString: String = Base64.encodeToString(queryJson.toByteArray(), 0)
        return runAPI(wasm.getTxData(contractAddr, encodedString))
    }
}

interface Wasm {
    @GET("/cosmwasm/wasm/v1/contract/{contractAddr}/smart/{queryData}")
    fun getTxData(
        @Path("contractAddr") contractAddr: String,
        @Path("queryData") queryData: String,
    ): Call<APIReturn.SmartQuery>
}
