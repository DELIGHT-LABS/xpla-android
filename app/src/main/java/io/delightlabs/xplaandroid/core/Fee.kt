package io.delightlabs.xplaandroid.core

import com.google.gson.annotations.SerializedName
import cosmos.tx.v1beta1.TxOuterClass.Fee
import java.io.Serializable

data class StdFee(
    var amount: List<Coin> = listOf(),
    var gas: String = ""
) : Serializable {
    constructor(fee: Fee) : this() {
        this.amount = fee.amountList.map {
            Coin(amount = it.amount, denom = it.denom)
        }
        this.gas = fee.gasLimit.toString()
    }
}