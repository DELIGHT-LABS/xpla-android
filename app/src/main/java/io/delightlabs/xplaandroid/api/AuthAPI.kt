package io.delightlabs.xplaandroid.api

class AuthAPI(val apiRequester: APIRequester) {

    fun accountInfo(address: String): APIReturn.Account? {
        return apiRequester.request<APIReturn.AccountReturn>(
            HttpMethod.GET,
            Endpoint.AccountInfo(address).path)?.account
    }
}

