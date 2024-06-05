package io.delightlabs.xplaandroid

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.google.protobuf.Any
import com.google.protobuf.kotlin.toByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import cosmos.bank.v1beta1.msgSend
import cosmos.base.v1beta1.CoinOuterClass
import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmos.tx.v1beta1.TxOuterClass
import cosmwasm.wasm.v1.msgExecuteContract
import cosmos.tx.v1beta1.TxOuterClass.Fee
import io.delightlabs.xplaandroid.api.APIRequester
import io.delightlabs.xplaandroid.api.APIReturn
import io.delightlabs.xplaandroid.api.HttpMethod
import io.delightlabs.xplaandroid.api.TxAPI
import io.delightlabs.xplaandroid.api.XplaNetwork
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import wallet.core.jni.PrivateKey
import java.math.BigDecimal
import java.util.Base64


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    private val lcd = LCDClient(
        network = XplaNetwork.TestNet,
        gasAdjustment = "3",
        gasPrices = listOf(CoinOuterClass.Coin.newBuilder().apply {
            this.amount = "850000000000"
            this.denom = "axpla"
        }.build())
    )

//    @Test
//    fun useAppContext() {
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("io.delightlabs.xplaandroid", appContext.packageName)
//    }

    @Test
    fun lcdWallet() {
        val seedPhrase =
            "table dinner sibling crisp hen genuine wing volume sport north omit cushion struggle script dinosaur merge medal visa also mixture faint surge boy wild"
        val wallet = lcd.wallet(seedPhrase)
        print(wallet.privateKey)
        val wallet2 = lcd.wallet(PrivateKey(wallet.privateKey.data()))
        print(wallet2.address)
        assertEquals("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9", wallet.address)
    }

    @Test
    fun lcdAuth() {
        val response: APIReturn.Account? =
            lcd.authAPI.accountInfo("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9")
        response?.baseAccount?.let { println("public address: ${it.address}") }
    }

    data class Query(val pairs: Pairs)
    data class Pairs(val limit: Int)

    @Test
    fun lcdWasm() {
        val query = Query(Pairs(20))
        val gson = Gson()
        val json = gson.toJson(query)
        val response: APIReturn.PairResponse? = lcd.wasmAPI.contractQuery(
            "xpla1j4kgjl6h4rt96uddtzdxdu39h0mhn4vrtydufdrk4uxxnrpsnw2qug2yx2",
            json.toString()
        )
        response?.let { println("wasm smartQuery:: ${it.data.pairs[0]}") }
    }

    @Test
    fun balanceAPI() {
        val response: APIReturn.BalanceReturn? =
            lcd.bankAPI.balance("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9")
        response?.let { println("balance response: $response") }
    }

    @Test
    fun networkTest() {
        val apiRequester = APIRequester(XplaNetwork.TestNet)
        val result = apiRequester.request<APIReturn.AccountReturn>(
            HttpMethod.GET,
            "/cosmos/auth/v1beta1/accounts/xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9"
        )
        if (result != null) {
            println("result: $result")
        }
    }

    @Test
    fun testCreateTx() {
        val seedPhrase =
            "table dinner sibling crisp hen genuine wing volume sport north omit cushion struggle script dinosaur merge medal visa also mixture faint surge boy wild"
        lcd.wallet(mnemonic = seedPhrase).let {
            val offerAmount = 1000000000000000000


            val sendCoin = Coin.newBuilder()
                .setAmount("$offerAmount")
                .setDenom("axpla")
                .build()

            val txSend = msgSend {
                this.toAddress = it.address
                this.fromAddress = it.address
                this.amount.add(sendCoin)
            }

            val any = Any.newBuilder()
                .setTypeUrl("/cosmos.bank.v1beta1.MsgSend")
                .setValue(txSend.toByteString())
                .build()

            val createTx = it.createAndSignTx(
                CreateTxOptions(msgs = listOf(any))
            )

            val broadcastRes = txAPI.broadcast(createTx)
            println("broadcastRes: $broadcastRes")
        }
    }

    @Test
    fun testSign() {
        val seedPhrase = "segment symbol pigeon tourist shop brush enter combine tornado pole snow federal lobster reopen drama wagon company salmon comfort rural palm fiscal crack roof"
        val lcd = LCDClient(
            XplaNetwork.LocalNet,
            gasAdjustment = "1",
            gasPrices = listOf()
        )
        lcd.wallet(mnemonic = seedPhrase).let {
            val sendCoin = Coin.newBuilder()
                .setAmount("1")
                .setDenom("axpla")
                .build()

            val txSend = msgSend {
                this.toAddress = "xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9"
                this.fromAddress = "xpla1nns26tapuzt36vdz0aadk7svm8p6xndtmwlyg8"
                this.amount.add(sendCoin)
            }

            val msg = Any.newBuilder()
                .setTypeUrl("/cosmos.bank.v1beta1.MsgSend")
                .setValue(txSend.toByteString())
                .build()

            val createTx = it.createAndSignTx(
                CreateTxOptions(
                    msgs = listOf(msg),
                    fee = Fee.newBuilder().setGasLimit(200000).build(),
                    sequence = 1),
                accountNumber = 0
            )

            assertEquals("RVlBVVTX6Sp2+2B1DBjsNBbHGxBJZUxAy1nPbiEQ4GkyRDe+lytfFg0Q105cIPQnt59Z8fpRhhb853fgSl5PWwA=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
           createTx.getSignatures(0)
        }
    }

    @Test
    fun testSwapCW20() {

        val seedPhrase =
            "table dinner sibling crisp hen genuine wing volume sport north omit cushion struggle script dinosaur merge medal visa also mixture faint surge boy wild"
        lcd.wallet(mnemonic = seedPhrase).let { wallet ->
            println(wallet.address)
            val offerAmount = "1000000" // 1 ctxt
            // cw20 -> anything
            val contractAddr = "xpla123dl27tlhs4lyvywarnrydtdr8kca2fy2r75ck3qzpupy5dsdwvqxu5vy0"
            val send = mapOf(
                "send" to mapOf(
                    "contract" to contractAddr,
                    "amount" to offerAmount,
                    "msg" to "eyJzd2FwIjp7fX0="
                )
            )

            Log.d("send", "$send")
            val jsonString = Gson().toJson(send)
            Log.d("transferJsonString", jsonString)
            val msgExecute = msgExecuteContract {
                this.sender = wallet.address // 보내는 주소
                this.contract =
                    "xpla19w8vmg7tmh07ztr3v7lq8sdny6jjkj6pk03a7fk52gpgepfxnlgq8g7r50" // token address (from) : ctxt
                this.msg = jsonString.toByteStringUtf8()
                this.funds
            }

            val any = Any.newBuilder()
                .setTypeUrl("/cosmwasm.wasm.v1.MsgExecuteContract")
                .setValue(msgExecute.toByteString())
                .build()

            val createTx = wallet.createAndSignTx(
                CreateTxOptions(msgs = listOf(any))
            )

            val broadcastRes = txAPI.broadcast(createTx)
            println("broadcastRes: $broadcastRes")
        }
    }

    @Test
    fun testSwapNativeToken() {

        val seedPhrase =
            "table dinner sibling crisp hen genuine wing volume sport north omit cushion struggle script dinosaur merge medal visa also mixture faint surge boy wild"
        lcd.wallet(mnemonic = seedPhrase).let { wallet ->
            println(wallet.address)
            val offerAmount = "10000000000000000000" // 10xpla
            val denom = "axpla"
            // native token -> anything
            val sendCoin = Coin.newBuilder()
                .setAmount(offerAmount)
                .setDenom(denom)
                .build()

            val swap = mapOf(
                "offer_asset" to mapOf(
                    "amount" to offerAmount,
                    "info" to mapOf(
                        "native_token" to mapOf(
                            "denom" to denom
                        )
                    )
                )
            )
            Log.d("swap", "$swap")
            val jsonString = Gson().toJson(swap)
            Log.d("transferJsonString", jsonString)
            val msgExecute = msgExecuteContract {
                this.sender = wallet.address // 보내는 주소
                this.contract = "xpla123dl27tlhs4lyvywarnrydtdr8kca2fy2r75ck3qzpupy5dsdwvqxu5vy0"
                this.msg = jsonString.toByteStringUtf8()
                this.funds.add(sendCoin)
            }


            val any = Any.newBuilder()
                .setTypeUrl("/cosmwasm.wasm.v1.MsgExecuteContract")
                .setValue(msgExecute.toByteString())
                .build()

            val createTx = wallet.createAndSignTx(
                CreateTxOptions(msgs = listOf(any))
            )

            val broadcastRes = txAPI.broadcast(createTx)
            println("broadcastRes: $broadcastRes")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testSimulation() {
        // 0a21036e3d6c03d524ec795042d53213d8dc9fbc9ce8664f8f7383f74e562e759d4d83
        val walletAddress =
            "xpla1fct20w8zcu8ha9880p3wy8r7r8p2hagc4pfe30"/* your wallet address here */
//        val walletLCD =  /* your walletLCD instance here */
        val res = lcd.authAPI.accountInfo(walletAddress)
        println("res: $res")
        if (res != null) {
            println("res: $res")

            val accountNumber = res.baseAccount.accountNumber.toIntOrNull()
            val sequence = res.baseAccount.sequence.toIntOrNull()
            val publicKey = res.baseAccount.getPublicKey()
            if (accountNumber != null && sequence != null && publicKey != null) {
                val signerDatas = listOf(
                    TxOuterClass.SignerInfo.newBuilder().apply {
                        this.sequence = sequence.toLong()
                        this.publicKey = Any.newBuilder().apply {
                            this.value =
                                "0a21${base64ToHex(publicKey)}".hexToByteArray().toByteString()
                            this.typeUrl = PubkeyProtoType
                        }.build()
                    }.build()
                )

                val offerAmount = 1000000000000000000

                val sendCoin = Coin.newBuilder()
                    .setAmount("$offerAmount")
                    .setDenom("axpla")
                    .build()

                val txSend = msgSend {
                    this.toAddress = walletAddress
                    this.fromAddress = walletAddress
                    this.amount.add(sendCoin)
                }

                val any = Any.newBuilder()
                    .setTypeUrl("/cosmos.bank.v1beta1.MsgSend")
                    .setValue(txSend.toByteString())
                    .build()


                val fee = TxAPI(lcd).estimateFee(signerDatas, CreateTxOptions(msgs = listOf(any)))

                if (fee.amountList.isNotEmpty()) {
                    val amount = fee.amountList[0]
                    println("amount: $amount")
                    val denom = amount.denom
                    if (denom == "axpla") {
                        val feeAmount = BigDecimal(amount.amount.toLongOrNull() ?: 0)
                        val feeString = feeAmount.divide(BigDecimal.TEN.pow(18)).toPlainString()
                        println("feeString:$feeString")
                    }
                }
            }
        }
    }
    fun base64ToHex(base64Str: String): String {
        val decodedBytes = Base64.getDecoder().decode(base64Str)
        return decodedBytes.joinToString("") { "%02x".format(it) }
    }
}

