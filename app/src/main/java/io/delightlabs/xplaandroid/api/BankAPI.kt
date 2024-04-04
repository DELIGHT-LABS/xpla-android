package io.delightlabs.xplaandroid.api

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

typealias APIParams = Map<String, kotlin.Any?>
class BankAPI(private val retrofit: RetrofitConnection): BaseAPI(retrofit) {

    private val bank: Bank = retrofit.getInstance().create(Bank::class.java)
    fun balance (
        address: String,
        params: APIParams? = mapOf()
    ): APIReturn.BalanceReturn? {
        return runAPI(bank.getBalance(address, params))
    }

    fun balance (
        address: String,
        pageOptions: PaginationOptions?
    ): APIReturn.BalanceReturn? {
        return runAPI(bank.getBalance(address, pageOptions?.dictionary))
    }

    fun balanceByDenom(
        address: String,
        denom: String,
        params: APIParams?
    ): APIReturn.Balance? {
        return runAPI(bank.getBalanceByDenom(address, denom, params))
    }

    fun total (
        params: APIParams? = mapOf()
    ): APIReturn.TotalReturn? {
        return runAPI(bank.getTotal(params))
    }

    fun total (
        pageOptions: PaginationOptions?
    ): APIReturn.TotalReturn? {
        return runAPI(bank.getTotal(pageOptions?.dictionary))
    }

}

interface Bank {
    @GET("/cosmos/bank/v1beta1/balances/{address}")
    fun getBalance(
        @Path("address") address: String,
        @Body params: APIParams?
    ): Call<APIReturn.BalanceReturn>

    @GET("/cosmos/bank/v1beta1/balances/{address}/by_denom")
    fun getBalanceByDenom(
        @Path("address") address: String,
        @Query("denom") denom: String,
        @Body params: APIParams?
    ): Call<APIReturn.Balance>

    @GET("/cosmos/bank/v1beta1/supply")
    fun getTotal(
        @Body params: APIParams?
    ): Call<APIReturn.TotalReturn>

}