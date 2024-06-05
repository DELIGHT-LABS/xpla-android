package io.delightlabs.xplaandroid.core
import java.io.Serializable

data class Coin(
    var amount: String = "",
    var denom: String = ""
) : Serializable