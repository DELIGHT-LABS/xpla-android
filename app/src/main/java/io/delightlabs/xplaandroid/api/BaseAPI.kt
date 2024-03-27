package io.delightlabs.xplaandroid.api


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
    val dictionary: HashMap<String, String>
        get() {
            val dic = hashMapOf<String, String>()
            limit?.let { dic["limit"] = it }
            offset?.let { dic["offset"] = it }
            key?.let { dic["key"] = it }
            countTotal?.let { dic["count_total"] = it.toString() }
            reverse?.let { dic["reverse"] = it.toString() }
            orderBy?.let { dic["order_by"] = it.value }
            return dic
        }
}


open class BaseAPI (private val apiRequester: APIRequester){
}