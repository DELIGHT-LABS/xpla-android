package io.delightlabs.xplaandroid.amino
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.protobuf.Any
import cosmos.bank.v1beta1.Tx
import java.io.Serializable
import io.delightlabs.xplaandroid.core.bank.v1beta1.MsgSend

data class AminoType(
    @SerializedName("type")
    var type: String = "",

    @SerializedName("value")
    var value: ProtocolAminoMsg
) : Serializable {
    fun serializedData(): ByteArray {
        val gson = Gson()
        return gson.toJson(this).toByteArray()
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
                val msg = Tx.MsgSend.parseFrom(protoMsg.value.toByteArray())
                val aminoMsg = MsgSend(msg)
                return aminoMsg.toAmino()
            }
            else -> {
                throw Exception("Can't find proto")
            }
        }
    }
}