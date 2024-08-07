package io.delightlabs.xplaandroid.amino
import com.google.gson.annotations.SerializedName
import com.google.protobuf.Any
import io.delightlabs.xplaandroid.GsonSingleton
import java.io.Serializable
import io.delightlabs.xplaandroid.core.bank.v1beta1.MsgSend
import io.delightlabs.xplaandroid.core.wasm.v1.MsgExecuteContract

data class AminoType(
    @SerializedName("type")
    var type: String = "",

    @SerializedName("value")
    var value: ProtocolAminoMsg
) : Serializable {
    fun serializedData(): ByteArray {
        return GsonSingleton.gson.toJson(this).toByteArray()
    }
}

interface ProtocolAminoMsg : Serializable {
    fun toAmino(): AminoType
}

data class AminoMsg(
    var protoMsg: Any
) : ProtocolAminoMsg {
    override fun toAmino(): AminoType {
        when (protoMsg.typeUrl) {
            "/cosmos.bank.v1beta1.MsgSend" -> {
                val msg = cosmos.bank.v1beta1.Tx.MsgSend.parseFrom(protoMsg.value.toByteArray())
                val aminoMsg = MsgSend(msg)
                return aminoMsg.toAmino()
            }
            "/cosmwasm.wasm.v1.MsgExecuteContract" -> {
                val msg = cosmwasm.wasm.v1.Tx.MsgExecuteContract.parseFrom(protoMsg.value.toByteArray())
                val aminoMsg = MsgExecuteContract(msg)
                return aminoMsg.toAmino()
            }
            else -> {
                throw Exception("Can't find proto")
            }
        }
    }
}