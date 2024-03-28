package io.delightlabs.xplaandroid.api

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Response
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class AuthAPI(private val retrofit: RetrofitConnection): BaseAPI(retrofit) {

    private val auth: Auth = retrofit.getInstance().create(Auth::class.java)
    fun accountInfo (address: String): APIReturn.Account? {
        return runAPI(auth.getUser(address))?.account
    }
}

interface Auth {
    @GET("/cosmos/auth/v1beta1/accounts/{address}")
    fun getUser(
        @Path("address") address: String
    ): Call<APIReturn.AccountReturn>
}