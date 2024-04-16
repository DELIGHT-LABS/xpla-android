package io.delightlabs.xplaandroid.api

import android.util.Base64

class WasmAPI(private val apiRequester: APIRequester) {

    fun contractQuery(
        contractAddr: String,
        queryJson: String
    ): APIReturn.SmartQuery? {
        val encodedString: String = Base64.encodeToString(queryJson.toByteArray(), 0)
        return apiRequester.request<APIReturn.SmartQuery>(
            HttpMethod.GET,
            Endpoint.SmartQuery(contractAddr, encodedString.trim()).path)
    }
}
