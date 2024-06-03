package io.delightlabs.xplaandroid.api

import android.util.Base64

class WasmAPI(val apiRequester: APIRequester) {

    inline fun <reified T> contractQuery(
        contractAddr: String,
        queryJson: String
    ):T? {
        val encodedString: String = Base64.encodeToString(queryJson.toByteArray(), 0)
        return apiRequester.request<T>(
            HttpMethod.GET,
            Endpoint.SmartQuery(contractAddr, encodedString.trim()).path)
    }


}
