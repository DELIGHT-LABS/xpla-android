package io.delightlabs.xplaandroid.api

import android.icu.lang.UCharacter.GraphemeClusterBreak.L
import com.google.gson.Gson
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos.FeatureSet.JsonFormat
import com.google.protobuf.UInt64Value
import com.google.protobuf.kotlin.DslList
import com.google.protobuf.kotlin.toByteStringUtf8
import com.google.protobuf.option
import cosmos.bank.v1beta1.Tx
import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmos.base.v1beta1.coin
import cosmos.msg.v1.Msg.signer
import cosmos.tx.signing.v1beta1.Signing
import cosmos.tx.signing.v1beta1.Signing.SignMode
import cosmos.tx.v1beta1.AuthInfoKt
import cosmos.tx.v1beta1.FeeKt
import cosmos.tx.v1beta1.ModeInfoKt
import cosmos.tx.v1beta1.SignerInfoKt
import cosmos.tx.v1beta1.TxBodyKt
import cosmos.tx.v1beta1.TxKt
import cosmos.tx.v1beta1.TxOuterClass
import cosmos.tx.v1beta1.TxOuterClass.ModeInfo
import cosmos.tx.v1beta1.TxOuterClass.ModeInfo.Single
import cosmos.tx.v1beta1.TxOuterClass.TxBody
import cosmos.tx.v1beta1.authInfo
import cosmos.tx.v1beta1.fee
import cosmos.tx.v1beta1.modeInfo
import cosmos.tx.v1beta1.signerInfo
import cosmos.tx.v1beta1.tx
import cosmos.tx.v1beta1.txBody
import io.delightlabs.xplaandroid.CreateTxOptions
import io.delightlabs.xplaandroid.LCDClient
import io.delightlabs.xplaandroid.pubkeyProtoType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.internal.http.HttpMethod
import org.checkerframework.checker.units.qual.s
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import wallet.core.jni.proto.Cosmos.Fee
import wallet.core.jni.proto.Cosmos.SignerInfo
import java.lang.Exception
import kotlin.math.sign
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import org.checkerframework.checker.units.qual.t
import kotlin.math.cos

class TxAPI(private val lcdClient: LCDClient) : BaseAPI(lcdClient.apiRequester) {

    private val retrofit = lcdClient.apiRequester
    private val tx = cosmos.tx.v1beta1.TxOuterClass.Tx.newBuilder()
    private val body = cosmos.tx.v1beta1.TxOuterClass.TxBody.newBuilder()
    private val authInfo = TxOuterClass.AuthInfo.newBuilder()

    private val emptyFee = fee {
        this.amount.clear()
        this.gasLimit = 0
        this.payer = ""
        this.granter = ""
    }

    enum class BroadcastMode {
        BROADCAST_MODE_SYNC,
        BROADCAST_MODE_BLOCK,
        BROADCAST_MODE_ASYNC
    }

    data class SignerOptions(
        val address: String?,
        val sequenceNumber: Int?,
        val publicKey: String?
    )

    fun cosmos.tx.v1beta1.TxOuterClass.Tx.appendEmptySignatures(signers: List<TxOuterClass.SignerInfo>): TxOuterClass.Tx {
        signers.forEach {
            var signerInfo: TxOuterClass.SignerInfo?
            signerInfo = signerInfo {
                this.publicKey = it.publicKey
                this.sequence = it.sequence
                this.modeInfo = modeInfo {
                    this.single = TxOuterClass.ModeInfo.Single
                        .newBuilder()
                        .setMode(Signing.SignMode.SIGN_MODE_DIRECT)
                        .build()
                }
            }
            this.authInfo.toBuilder()
                .addSignerInfos(signerInfo)
                .build()

//            this.signaturesList.add(ByteString.EMPTY)
        }

        return this
    }

    fun create(
        signers: List<SignerOptions>,
        options: CreateTxOptions
    ): cosmos.tx.v1beta1.TxOuterClass.Tx {
        var fee: cosmos.tx.v1beta1.TxOuterClass.Fee? = null
        val msgs: List<com.google.protobuf.Any> = options.msgs
        var memo = options.memo
        val timeoutHeight = options.timeoutHeight
        var signerDatas: MutableList<TxOuterClass.SignerInfo> = mutableListOf()

        for (signer in signers) {
            var sequenceNumber = signer.sequenceNumber
            var publicKey = signer.publicKey

            if (sequenceNumber == null || publicKey == null) {
                signer.address?.let { address ->
                    lcdClient.authAPI.accountInfo(address)?.let { accountInfo ->
                        if (sequenceNumber == null) {
                            sequenceNumber = accountInfo.baseAccount.getSequenceNumber().toInt()
                        }

                        if (publicKey == null) {
                            publicKey = accountInfo.baseAccount.getPublicKey()
                        }
                    }
                }
            }

            sequenceNumber?.let { sequenceNumber ->
                publicKey?.let { publicKey ->
                    signerDatas.add(
                        signerInfo {
                            this.sequence = sequenceNumber.toLong()
                            this.publicKey = com.google.protobuf.Any.newBuilder()
                                .setValue("0a21$publicKey".toByteStringUtf8())
                                .setTypeUrl(pubkeyProtoType)
                                .build()
                        }
                    )
                }
            }
        }

        if (fee == null) {
            fee =
                estimateFee(
                    signerDatas,
                    options
                )
        }

        println("estimateFee \uD83E\uDD28: $fee")

        println("msgsResult \uD83E\uDD28: ${msgs.get(0)}")
        if (msgs.isEmpty()) {
            println("msgsIsEmpty \uD83E\uDD28: ${msgs.isEmpty()}")
            return tx {
                this.body = txBody {
                    this.messages.clear()
                    this.memo = ""
                    this.timeoutHeight = 0
                }

                this.authInfo = authInfo {
                    this.signerInfos.clear()
                    this.fee = fee
                }
            }
        }

        return tx {
            this.body = txBody {
                msgs.forEach { message ->
                    messages.add(message)
                }
                memo?.let {
                    this.memo = it
                }
                this.timeoutHeight = (timeoutHeight ?: 0).toLong()
            }

            this.authInfo = authInfo {
                this.signerInfos.clear()
                this.fee = fee
            }

            this.signatures.clear()
        }
    }

    fun estimateFee(
        signers: List<TxOuterClass.SignerInfo>,
        options: CreateTxOptions,
    ): TxOuterClass.Fee {
        val gasPrices = if (options.gasPrices != null) options.gasPrices else lcdClient.gasPrices
        val gasAdjustment =
            if (options.gasPrices != null) options.gasAdjustment else lcdClient.gasAdjustment
        val feeDenoms = options.feeDenoms
        val msgs = options.msgs
        var gas: String? = options.gas
        var gasPricesCoins: List<Coin> = listOf()

        println(
            "gasPrices \uD83E\uDD28: ${lcdClient.gasPrices.get(0).denom} ${
                lcdClient.gasPrices.get(
                    0
                ).amount
            }"
        )
        gasPrices?.let { gasPrices ->
            gasPricesCoins = gasPrices
            feeDenoms?.let { feeDenoms ->
                val gasPricesCoinsFiltered = gasPricesCoins.filter { c ->
                    feeDenoms.contains(c.denom)
                }

                if (gasPricesCoinsFiltered.isNotEmpty()) {
                    gasPricesCoins = gasPricesCoinsFiltered
                }
            }
        }

        println("gasMsgsResult \uD83E\uDD28: $msgs")
        if (msgs.isEmpty()) {
            return emptyFee
        }

        val txbody = txBody {
            msgs.forEach {
                this.messages.add(it)
            }
            this.memo = options.memo ?: ""
        }

        val authInfo = authInfo {
            this.signerInfos.clear()
            this.fee = fee {
                this.gasLimit = 0
                this.amount.clear()
            }
        }

        var tx = tx {
            this.body = txbody
            this.authInfo = authInfo
            this.signatures.clear()
        }

        println("authInfo fee gasLimit \uD83E\uDD28: ${tx.authInfo.fee.gasLimit}")
        println("authInfo fee Amount \uD83E\uDD28: ${tx.authInfo.fee}")
        println("body messages \uD83E\uDD28: ${body.messagesList}}")


        tx = tx.appendEmptySignatures(signers)

        println("gas \uD83E\uDD28: $gas")
        if (gas == null || gas == "auto" || gas == "0") {
            println("gasAdjustment \uD83E\uDD28: $gasAdjustment")
            gasAdjustment?.toInt().let {
                println("진입확인 \uD83E\uDD28")
                println("tx 왜 안나와 \uD83E\uDD28: $tx")
                println("authInfo fee gasLimit \uD83E\uDD28: ${tx.authInfo.fee.gasLimit}")
                println("authInfo fee Amount \uD83E\uDD28: ${tx.authInfo.fee.amountCount}")
                println("body messages \uD83E\uDD28: ${tx.body.getMessages(0)}")
                println("estimateGas \uD83E\uDD28: ${estimateGas(tx, it!!)}")
                println("gasAdjustment \uD83E\uDD28: ${it}")

                estimateGas(tx, it!!)?.let {
                    println("gas: 🤨${it.toString()}")
                    println("tx \uD83E\uDD28: ${tx.body.messagesList.isEmpty()}")
                    println("estimageGas \uD83E\uDD28: $it")
                    gas = it.toString()

                }
            }
        }

        if (gas == null) {
            return emptyFee
        }

        val feeAmount = gasPricesCoins.map {
            if (it.amount.toUIntOrNull() != null && gas?.toUIntOrNull() != null) {
                val coinAmount = it.amount.toULong()
                gas?.toULong()?.let {
                    val multipliedAmount = coinAmount * it
                }
            }
        }


        return fee {
            this.amount
            this.gasLimit = gas?.toLong() ?: 0
            this.payer = ""
            this.granter = ""
        }
    }

    fun estimateGas(
        tx: cosmos.tx.v1beta1.TxOuterClass.Tx,
        gasAdjustment: Int,
        signers: List<TxOuterClass.SignerInfo>? = null
    ): Int {
        var simTx = tx
        println("tx 하하하 \uD83E\uDD28: ${tx.authInfo}")
        println("signatureIsEmpty ${tx.signaturesList.isEmpty()}")
        if (tx.signaturesList.isEmpty()) {
            println("signers: $signers")
            signers?.let {
                if (signers.isNotEmpty()) {
                    // 에러처리
                }

                val authInfo = authInfo {
                    this.signerInfos.clear()
                    this.fee = fee {
                        this.amount.clear()
                        this.gasLimit = 0
                    }
                }

                simTx = tx {
                    this.body = tx.body
                    this.authInfo = authInfo
                    this.signatures.clear()
                }
                simTx = simTx.appendEmptySignatures(signers)
            }
        }

        println("signers \uD83E\uDD28: ${signers}")
        println("simTx \uD83E\uDD28: ${simTx.body.messagesList} ${simTx.authInfo.signerInfosList} ${simTx.signaturesList}")

        val simulateRes = lcdClient.apiRequester.test2(
            "cosmos/tx/v1beta1/simulate", hashMapOf<String, kotlin.Any>(
                "tx_bytes" to Base64.getEncoder().encodeToString(simTx.toByteArray())
            )
        )

        println("simulateResult \uD83E\uDD28: ${simulateRes}")

        return 0
    }

    fun broadcast(tx: cosmos.tx.v1beta1.TxOuterClass.Tx): Retrofit? {

        var encodedTxBytes = Base64.getEncoder().encodeToString(tx.toByteArray())

        val params = hashMapOf<String, kotlin.Any>(
            "tx_bytes" to encodedTxBytes,
            "mode" to "BROADCAST_MODE_SYNC"
        )

        return lcdClient.apiRequester.test("/cosmos/tx/v1beta1/txs", params)
    }


}
