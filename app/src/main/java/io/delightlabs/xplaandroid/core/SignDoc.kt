package io.delightlabs.xplaandroid.core

import com.google.gson.annotations.SerializedName
import cosmos.tx.v1beta1.TxOuterClass.AuthInfo
import cosmos.tx.v1beta1.TxOuterClass.SignDoc
import cosmos.tx.v1beta1.TxOuterClass.TxBody
import io.delightlabs.xplaandroid.amino.AminoMsg
import io.delightlabs.xplaandroid.amino.AminoType
import java.io.Serializable

data class StdSignDoc(
    @SerializedName("account_number")
    var accountNumber: String = "",

    @SerializedName("sequence")
    var sequence: String = "",

    @SerializedName("chain_id")
    var chainID: String = "",

    @SerializedName("memo")
    var memo: String = "",

    @SerializedName("fee")
    var fee: StdFee = StdFee(),

    @SerializedName("msgs")
    var msgs: List<AminoType> = listOf()
) : Serializable {
    constructor(signDoc: SignDoc) : this() {
        this.accountNumber = signDoc.accountNumber.toString()

        AuthInfo.parseFrom(signDoc.authInfoBytes).let {
            this.sequence = it.signerInfosList[0].sequence.toString()
            this.fee = StdFee(fee = it.fee, chainID = signDoc.chainId)
        }

        TxBody.parseFrom(signDoc.bodyBytes).let {
            this.memo = it.memo
            this.msgs = it.messagesList.map {
                AminoMsg(protoMsg = it).toAmino()
            }
        }

        this.chainID = signDoc.chainId
    }
}