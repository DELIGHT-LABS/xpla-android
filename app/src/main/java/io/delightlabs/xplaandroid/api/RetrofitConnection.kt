package io.delightlabs.xplaandroid.api

import okhttp3.internal.http.HttpMethod
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import wallet.core.jni.Hash
import java.io.Serializable
import java.lang.Exception

interface Network {
    val url: String
    val chainId: String
}
enum class XplaNetwork: Network {
    TestNet {
        override val url: String
            get() = "https://cube-lcd.xpla.dev"
        override val chainId: String
            get() = "cube_47-5"
    },
    MainNet {
        override val url: String
            get() = "https://dimension-lcd.xpla.dev"
        override val chainId: String
            get() = "dimension_37-1"

    }

}

typealias APIParams = Map<String, Any?>

interface SimulateAPIService {
    @POST("cosmos/tx/v1beta1/simulate")
    fun simultate (
        @Body params: HashMap<String, Any>
    ): Call<APIReturn.SimulateTx>
}
interface BroadCastAPIService {
    @POST("/cosmos/tx/v1beta1/txs")
    fun broadcastTransaction (
        @Body params: HashMap<String, Any>
        ): Call<APIReturn.BroadcastResponse>
}

class RetrofitConnection(private val network: XplaNetwork) {

    private val url = network.url
    private var instance: Retrofit? = null

    fun getInstance(): Retrofit {
        if(instance == null) {  // null인 경우에만 생성
            instance = Retrofit.Builder()
                .baseUrl(url)  // API 베이스 URL 설정
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return instance!!
    }
}