package io.delightlabs.xplaandroid.api

import android.util.Log
import io.delightlabs.xplaandroid.GsonSingleton
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

typealias APIParams = HashMap<String, String>?

interface Grpc {
    val url: String
    val noTLS: Boolean
}

interface Network {
    val url: String
    val chainId: String
    val grpc: Grpc
}

enum class XplaNetwork : Network {
    Testnet {
        override val url: String
            get() = "https://cube-lcd.xpla.dev"
        override val chainId: String
            get() = "cube_47-5"
        override val grpc: Grpc
            get() = object : Grpc {
                override val url: String = "xpla-grpc.publicnode.com:443"
                override val noTLS: Boolean = false
            }
    },
    Mainnet {
        override val url: String
            get() = "https://dimension-lcd.xpla.dev"
        override val chainId: String
            get() = "dimension_37-1"
        override  val grpc: Grpc
            get() = object : Grpc {
                override val url: String = "xpla-grpc.publicnode.com:443"
                override val noTLS: Boolean = false
            }
    },
    Localnet {
        override val url: String
            get() = "http://localhost:1317"
        override val chainId: String
            get() = "n_1-1"
        override val grpc: Grpc
            get() = object : Grpc {
                override val url: String = "http://localhost:9090"
                override val noTLS: Boolean = true
            }
    }

}

enum class HttpMethod {
    PUT,
    POST,
    GET,
    DELETE
}

class APIRequester(private val network: XplaNetwork) {

    private val baseUrl = URL(network.url).toURI()

    fun buildURL(endpoint: String): URL {
        return baseUrl.resolve(endpoint).toURL()
    }

    inline fun <reified T> request(
        httpMethod: HttpMethod,
        endpoint: String,
        param: APIParams = null
    ): T? {
        val httpURLConnection: HttpURLConnection =
            buildURL(endpoint).openConnection() as HttpURLConnection
        runCatching {
            httpURLConnection.requestMethod = httpMethod.toString()
            httpURLConnection.setRequestProperty("Content-Type", "application/json")

            if (param != null && httpMethod == HttpMethod.POST) {
                // 서버에서 온 데이터를 출력할 수 있는 상태인지
                httpURLConnection.doOutput = true

                // HashMap을 JSON 문자열로 변환
                val jsonInputString = GsonSingleton.gson.toJson(param)

                OutputStreamWriter(httpURLConnection.outputStream, "UTF-8").use { writer ->
                    writer.write(jsonInputString)
                    writer.flush()
                }
            }
            httpURLConnection.connect()
        }.onSuccess {
            when (httpURLConnection.responseCode) {
                200, 201 -> {
                    val br = BufferedReader(InputStreamReader(httpURLConnection.inputStream))
                    val sb = StringBuilder()
                    var output: String?
                    while (br.readLine().also { output = it } != null) {
                        sb.append(output + "\n")
                    }
                    return GsonSingleton.gson.fromJson(sb.toString(), T::class.java)
                }
            }
        }.onFailure {
            when (it) {
                is MalformedURLException -> {
                    Log.d(javaClass.name, it.toString())
                    throw it
                }

                is IOException -> {
                    Log.d(javaClass.name, it.toString())
                    throw it
                }
            }
        }.also {
            runCatching { httpURLConnection.disconnect() }.onFailure {
                throw it
            }
        }.getOrThrow()
        return null
    }
}