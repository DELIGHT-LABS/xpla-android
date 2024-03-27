package io.delightlabs.xplaandroid.api

import android.util.Log
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

typealias APIParams = HashMap<String, String>?
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
enum class HttpMethod {
    PUT,
    POST,
    GET,
    DELETE
}

class APIRequester(private val network: XplaNetwork) {

    private val baseUrl = URL(network.url).toURI()
    val gson = Gson()

    fun buildURL(endpoint: String): URL {
        return baseUrl.resolve(endpoint).toURL()
    }



    inline fun <reified T> request(httpMethod: HttpMethod, endpoint: String, param: APIParams = null): T? {
        var c: HttpURLConnection? = null
        try {
            c = buildURL(endpoint).openConnection() as HttpURLConnection
            c.requestMethod = httpMethod.toString()
            c.setRequestProperty("Content-Type", "application/json")

            if (param != null && httpMethod == HttpMethod.POST) {
                // 서버에서 온 데이터를 출력할 수 있는 상태인지
                c.doOutput = true

                // HashMap을 JSON 문자열로 변환
                val jsonInputString = gson.toJson(param)

                OutputStreamWriter(c.outputStream, "UTF-8").use { writer ->
                    writer.write(jsonInputString)
                    writer.flush()
                }
            }

            c.connect()
            when (c.responseCode) {
                200, 201 -> {
                    val br = BufferedReader(InputStreamReader(c.inputStream))
                    val sb = StringBuilder()
                    var output: String?
                    while (br.readLine().also { output = it } != null) {
                        sb.append(output + "\n")
                    }
                    return gson.fromJson(sb.toString(), T::class.java)
                }
            }
        } catch (ex: MalformedURLException) {
            Log.d(javaClass.name, ex.toString() )
            throw ex
        } catch (ex: IOException) {
            Log.d(javaClass.name, ex.toString() )
            throw ex
        } finally {
            if (c != null) {
                try {
                    c.disconnect()
                } catch (ex: Exception) {
                    throw ex
                }
            }
        }
        return null
    }


}