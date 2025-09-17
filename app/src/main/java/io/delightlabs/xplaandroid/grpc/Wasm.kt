import android.util.Base64
import android.util.Log
import com.google.gson.JsonParser
import com.google.protobuf.ByteString
import cosmwasm.wasm.v1.QueryGrpc
import cosmwasm.wasm.v1.QueryOuterClass.*
import io.delightlabs.xplaandroid.GsonSingleton
import io.delightlabs.xplaandroid.api.APIReturn
import io.grpc.ManagedChannel

class WasmGRPC(val channel: ManagedChannel) {
    private val stub: QueryGrpc.QueryBlockingStub = QueryGrpc.newBlockingStub(channel)

    fun contractQueryTokenInfo(contractAddr: String): APIReturn.TokenInfoResponse? {
        val queryJson = "{\"token_info\":{}}"
        val response = smartContractState(contractAddr, queryJson)
        val jsonString = response.data.toStringUtf8()
        val jsonObject = JsonParser.parseString(jsonString).asJsonObject
        val tokenInfoResult = APIReturn.TokenInfoResult(
            name = jsonObject.get("name").asString,
            symbol = jsonObject.get("symbol").asString,
            decimals = jsonObject.get("decimals").asInt,
            totalSupply = jsonObject.get("total_supply").asString
        )
        return APIReturn.TokenInfoResponse(data = tokenInfoResult)
    }

    fun smartContractState(
        contractAddr: String,
        queryJson: String,
    ): QuerySmartContractStateResponse {
        val queryData = ByteString.copyFromUtf8(queryJson)

        val request = QuerySmartContractStateRequest.newBuilder()
            .setAddress(contractAddr)
            .setQueryData(queryData)
            .build()

        return stub.smartContractState(request)
    }
    inline fun <reified T> contractQuery(
        contractAddr: String,
        queryJson: String,
    ): T? {
        return try {
            val response = smartContractState(contractAddr, queryJson)
            val jsonString = response.data.toStringUtf8()
            GsonSingleton.gson.fromJson(jsonString, T::class.java)
        } catch (e: Exception) {
            null
        }
    }
}