package io.delightlabs.xplaandroid.api

import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import java.net.URLEncoder

class WasmAPI(val apiRequester: APIRequester) {

    inline fun <reified T> contractQuery(
        contractAddr: String,
        queryJson: String
    ):T? {
        val encodedString: String = Base64.encodeToString(queryJson.toByteArray(), 0).trim().replace("\n","")
        val path = Endpoint.SmartQuery(contractAddr, encodedString).path
        return apiRequester.request<T>(
            HttpMethod.GET,
            path)
    }


}
