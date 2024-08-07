package io.delightlabs.xplaandroid.core.bank.v1beta1

import com.google.gson.annotations.SerializedName
import cosmos.bank.v1beta1.Tx
import io.delightlabs.xplaandroid.amino.AminoType
import io.delightlabs.xplaandroid.amino.ProtocolAminoMsg
import io.delightlabs.xplaandroid.core.Coin
import java.io.Serializable


data class MsgSend(

    @SerializedName("from_address")
    var fromAddress: String = "",
    @SerializedName("to_address")
    var toAddress: String = "",
    var amount: List<Coin> = listOf()
) : ProtocolAminoMsg, Serializable {

    constructor(proto: Tx.MsgSend) : this(
    fromAddress = proto.fromAddress,
    toAddress = proto.toAddress,
    amount = proto.amountList.map { Coin(it.amount, it.denom) }
    )

    override fun toAmino(): AminoType {
        return AminoType(
            type = "cosmos-sdk/MsgSend",
            value = this)
    }
}