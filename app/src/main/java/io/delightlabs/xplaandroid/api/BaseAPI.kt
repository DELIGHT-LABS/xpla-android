package io.delightlabs.xplaandroid.api

import retrofit2.Call
import java.lang.Exception


enum class OrderBy(val value: String) {
    ASCENDING("asc"),
    DESCENDING("desc")
}
data class PaginationOptions(
    var limit: String? = null,
    var offset: String? = null,
    var key: String? = null,
    var countTotal: Boolean? = null,
    var reverse: Boolean? = null,
    var orderBy: OrderBy? = null
) {
    val dictionary: Map<String, Any>
        get() {
            val dic = mutableMapOf<String, Any>()
            limit?.let { dic["limit"] = it }
            offset?.let { dic["offset"] = it }
            key?.let { dic["key"] = it }
            countTotal?.let { dic["count_total"] = it }
            reverse?.let { dic["reverse"] = it }
            orderBy?.let { dic["order_by"] = it.value }
            return dic
        }
}


open class BaseAPI (private val retrofit: RetrofitConnection){
    val retrofitConnection = retrofit

    fun <T> runAPI(api: Call<T>): T? {
        val response = api.execute()
        if (!response.isSuccessful) {
            throw Exception(response.code().toString())
        }
        return response.body()
    }
}