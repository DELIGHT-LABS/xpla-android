package io.delightlabs.xplaandroid.core.wasm.v1

import cosmwasm.wasm.v1.Tx
import io.delightlabs.xplaandroid.GsonSingleton
import io.delightlabs.xplaandroid.amino.AminoType
import io.delightlabs.xplaandroid.amino.ProtocolAminoMsg
import io.delightlabs.xplaandroid.core.Coin
import java.io.Serializable

data class MsgExecuteContract(
    var sender: String = "",
    var contract: String = "",
    var msg: Cw20ExecuteMsg,
    var funds: List<Coin> = listOf()
) : ProtocolAminoMsg, Serializable {

    constructor(tx: Tx.MsgExecuteContract): this(
        sender = tx.sender,
        contract = tx.contract,
        msg = GsonSingleton.gson.fromJson(tx.msg.toStringUtf8(), Cw20ExecuteMsg::class.java),
        funds = tx.fundsList.map { Coin(it.amount, it.denom) }
    )

    override fun toAmino(): AminoType {
        return AminoType(
            type = "wasm/MsgExecuteContract",
            value = this)
    }
}

data class Cw20ExecuteMsg (
    var transfer: TransferMsg
) : Serializable

data class TransferMsg (
    var recipient: String = "",
    var amount: String = ""
): Serializable