package io.delightlabs.xplaandroid.api

class BankAPI(private val apiRequester: APIRequester) {

    fun balance(
        address: String,
        params: APIParams = hashMapOf()
    ): APIReturn.BalanceReturn? {

        return apiRequester.request<APIReturn.BalanceReturn>(
            HttpMethod.GET,
            Endpoint.Balance(address).path,
            params
        )
    }

    fun balance(
        address: String,
        pageOptions: PaginationOptions?
    ): APIReturn.BalanceReturn? {

        return apiRequester.request<APIReturn.BalanceReturn>(
            HttpMethod.GET,
            Endpoint.Balance(address).path,
            pageOptions?.dictionary
        )
    }

    fun balanceByDenom(
        address: String,
        denom: String,
        params: APIParams = hashMapOf()
    ): APIReturn.Balance? {

        return apiRequester.request<APIReturn.Balance>(
            HttpMethod.GET,
            Endpoint.BalanceByDenom(address, denom).path,
            params
        )
    }

    fun total(
        params: APIParams = hashMapOf()
    ): APIReturn.TotalReturn? {

        return apiRequester.request<APIReturn.TotalReturn>(
            HttpMethod.GET,
            Endpoint.BankTotal().path,
            params
        )
    }

    fun total(
        pageOptions: PaginationOptions?
    ): APIReturn.TotalReturn? {

        return apiRequester.request<APIReturn.TotalReturn>(
            HttpMethod.GET,
            Endpoint.BankTotal().path,
            pageOptions?.dictionary
        )
    }

}
