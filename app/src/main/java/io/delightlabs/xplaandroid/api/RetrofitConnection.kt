package io.delightlabs.xplaandroid.api

import okhttp3.internal.http.HttpMethod
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
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

interface APIService {
    @POST("{endpoint}")
    fun broadcastTransaction (
        @Path("endpoint") endpoint: String,
        @Body params: HashMap<String, Any>
        ): Call<APIReturn.BroadcastResponse>
}
class RetrofitConnection(private val network: XplaNetwork) {

    private val url = network.url

    private var instance: Retrofit? = null
    fun test(endpoint: String, params: HashMap<String, Any>): Retrofit {

        println("url \uD83E\uDD28: $url")
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        println("retrofit \uD83E\uDD28: $retrofit")

        val apiService = retrofit.create(APIService::class.java)
        var endpoint = endpoint
        var params = params

        println("apiServe \uD83E\uDD28: $apiService")

        println("endpoint \uD83E\uDD28: $endpoint")
        println("params \uD83E\uDD28: $params")
        val call = apiService.broadcastTransaction(endpoint, params)

        println("call \uD83E\uDD28: $call")
        try {
            // 동기적으로 요청을 실행하고 응답을 받음
            val response = call.execute()
            println("statuscode \uD83E\uDD28: ${response.code()}")
            println("responseResult \uD83E\uDD28: ${response.isSuccessful}")
            if (response.isSuccessful) {
                val broadcastResponse = response.body()
                println("broadcastzz \uD83E\uDD28: $broadcastResponse")
            } else {
                println("zzz \uD83E\uDD28: error")
            }
        } catch (e: Exception) {
            println("Failed to execute request \uD83E\uDD28: ${e.message}")
        }
//        call.enqueue(object : Callback<APIReturn.BroadcastResponse> {
//            override fun onResponse(call: Call<APIReturn.BroadcastResponse>, response: Response<APIReturn.BroadcastResponse>) {
//                if (response.isSuccessful) {
//                    val broadcastResponse = response.body()
//                    println("broadcastzz: " + "$broadcastResponse")
//                } else {
//                    println("zzz: " + "error")
//                }
//            }
//
//            override fun onFailure(call: Call<APIReturn.BroadcastResponse>, t: Throwable) {
////                println("zzz: " + "error")
//            }
//        })
        return retrofit
    }
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