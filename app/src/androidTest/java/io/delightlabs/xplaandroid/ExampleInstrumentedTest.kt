package io.delightlabs.xplaandroid

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.TypeRegistry
import com.google.protobuf.util.JsonFormat
import cosmos.bank.v1beta1.msgSend
import cosmos.base.v1beta1.CoinOuterClass
import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmos.tx.signing.v1beta1.Signing.SignMode
import cosmos.tx.v1beta1.TxOuterClass.Fee
import cosmwasm.wasm.v1.msgExecuteContract
import io.delightlabs.xplaandroid.api.APIRequester
import io.delightlabs.xplaandroid.api.APIReturn
import io.delightlabs.xplaandroid.api.HttpMethod
import io.delightlabs.xplaandroid.api.XplaNetwork
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import wallet.core.jni.PrivateKey
import xpla.tx.Tx
import xpla.tx.createTxOptions


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    private val lcd = LCDClient(
        network = XplaNetwork.Testnet,
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
        val json = GsonSingleton.gson.toJson(query)
        val response: APIReturn.TokenInfoResponse? = lcd.wasmAPI.contractQueryTokenInfo(
            "xpla1w6hv0suf8dmpq8kxd8a6yy9fnmntlh7hh9kl37qmax7kyzfd047qnnp0mm",
        )
        println("wasm smartQuery:: $response")
        response?.let { println("wasm smartQuery:: ${it.data}") }
    }

    @Test
    fun balanceAPI() {
        val response: APIReturn.BalanceReturn? =
            lcd.bankAPI.balance("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9")
        response?.let { println("balance response: $response") }
    }

    @Test
    fun networkTest() {
        val apiRequester = APIRequester(XplaNetwork.Testnet)
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
        val seedPhrase = "table dinner sibling crisp hen genuine wing volume sport north omit cushion struggle script dinosaur merge medal visa also mixture faint surge boy wild"
        val lcdClient = LCDClient(
            network = XplaNetwork.Mainnet,
            gasAdjustment = "1.3",
            gasPrices = listOf(
                CoinOuterClass.Coin.newBuilder().apply {
                    this.amount = "850000000000"
                    this.denom = "axpla"
                }.build(),
            ),
        )

        lcdClient.wallet(mnemonic = seedPhrase).let { wallet ->
            val offerAmount = 1000000000000000000
            val sendCoin = Coin.newBuilder()
                .setAmount("$offerAmount")
                .setDenom("axpla")
                .build()

            val txSend = msgSend {
                this.toAddress = wallet.address
                this.fromAddress = wallet.address
                this.amount.add(sendCoin)
            }

            val any = Any.newBuilder()
                .setTypeUrl("/cosmos.bank.v1beta1.MsgSend")
                .setValue(txSend.toByteString())
                .build()

            val createTx = wallet.createAndSignTx(
                createTxOptions {
                    this.msgs.add(any)
                    gas = "100000"
                    this.fee = Fee.newBuilder()
                        .setGasLimit(100000)
                        .addAmount(
                            Coin.newBuilder()
                                .setAmount("85000000000000000")
                                .setDenom("axpla")
                                .build()
                        )
                        .build()
                    this.sequence = 1
                    }
            )

            assertEquals(
                "TZ4XEmv/XYF7bYYBMbIEt3tKmGPhclixbcgpKWPjUBUnkO9rEy472FG2S6p6l3SKSoq1KSj+Mlm/BVSexbJMrwE=",
                java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray())
            )
        }
    }

    @Test
    fun testSignAmino() {
        val seedPhrase = "segment symbol pigeon tourist shop brush enter combine tornado pole snow federal lobster reopen drama wagon company salmon comfort rural palm fiscal crack roof"
        val lcd = LCDClient(
            XplaNetwork.Localnet,
            gasAdjustment = "1.3",
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
                createTxOptions {
                    this.msgs.add(msg)
                    this.fee = Fee.newBuilder().addAmount(0, sendCoin).setGasLimit(200000).build()
                    sequence = 1
                },
                accountNumber = 0,
                signMode = SignMode.SIGN_MODE_LEGACY_AMINO_JSON
            )

            assertEquals("MEgb9Kz2awtey5G9rC2ptBTYlRqUN+qfS1Qz27r3tdsmNr6qU+L2fMupYfUvTUNlT4GPN+7SL5nfWkGixPa39QE=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
            createTx.getSignatures(0)
        }
    }

    @Test
    fun testSignAminoWithPayerWhenCosmos47() {
        val seedPhrase = "segment symbol pigeon tourist shop brush enter combine tornado pole snow federal lobster reopen drama wagon company salmon comfort rural palm fiscal crack roof"
        val lcd = LCDClient(
            XplaNetwork.Testnet,
            gasAdjustment = "1.3",
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
                createTxOptions{
                    this.msgs.add(msg)
                    this.fee = Fee.newBuilder().addAmount(0, sendCoin).setGasLimit(200000).setPayer("xpla1nns26tapuzt36vdz0aadk7svm8p6xndtmwlyg8").build()
                    this.sequence = 2
                },
                accountNumber = 0,
                signMode = SignMode.SIGN_MODE_LEGACY_AMINO_JSON
            )

            assertEquals("1WW5WdDSn/7DAImtpZxgggTMcWoQrxqGHUGyQUpuCFcsMEgAl3Vxgteye4DSyxVzlM5imp3eRp/UJsPYgnn/5AE=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
            createTx.getSignatures(0)
        }
    }

    @Test
    fun testSignAminoContractMsg() {
        val seedPhrase = "segment symbol pigeon tourist shop brush enter combine tornado pole snow federal lobster reopen drama wagon company salmon comfort rural palm fiscal crack roof"
        val lcd = LCDClient(
            XplaNetwork.Localnet,
            gasAdjustment = "1.3",
            gasPrices = listOf()
        )

        lcd.wallet(mnemonic = seedPhrase).let {
            val feeCoin = Coin.newBuilder()
                .setAmount("1")
                .setDenom("axpla")
                .build()

            val transferMsg = """{
                "transfer": {
                    "amount": "1",
                "recipient": "xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9"
            }
            }""".toByteArray()

            val msgExecute = msgExecuteContract {
                this.sender = "xpla1nns26tapuzt36vdz0aadk7svm8p6xndtmwlyg8"
                this.contract = "xpla14hj2tavq8fpesdwxxcu44rty3hh90vhujrvcmstl4zr3txmfvw9s525s0h"
                this.msg = ByteString.copyFrom(transferMsg)
            }

            val msg = Any.newBuilder()
                .setTypeUrl("/cosmwasm.wasm.v1.MsgExecuteContract")
                .setValue(msgExecute.toByteString())
                .build()

            val createTx = it.createAndSignTx(
                createTxOptions {
                    this.msgs.add(msg)
                    this.fee = Fee.newBuilder().addAmount(0, feeCoin).setGasLimit(200000).build()
                    this.sequence = 8
                },
                accountNumber = 0,
                signMode = SignMode.SIGN_MODE_LEGACY_AMINO_JSON
            )

            assertEquals("uuuGzlHcidASzxYRUtfldCjkPSq/YHUdPmUDl1Tl591a52zGqgniKLUSgzUEBUF+SetkcRtXOGJrB2bbXBUQ4QE=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
            createTx.getSignatures(0)
        }
    }

    @Test
    fun testSignAminoWithMemo() {
        val seedPhrase = "segment symbol pigeon tourist shop brush enter combine tornado pole snow federal lobster reopen drama wagon company salmon comfort rural palm fiscal crack roof"
        val lcd = LCDClient(
            XplaNetwork.Testnet,
            gasAdjustment = "1.5",
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

            val fee =
                Fee.newBuilder()
                    .setGasLimit(200000)
                    .addAmount(sendCoin)
                    .build()

            val createTx = it.createAndSignTx(
                createTxOptions {
                    this.memo = "U2FsdGVkX18bzNU4JzqtCxkderonQ0mZnkEFoCH/s9Thngscv/0s7hdQsdIzFDVFKK6PPJL1PYcAEfu46EVEjjLTaghGGQj9kncghM5YoV8="
                    this.msgs.add(msg)
                    this.fee = fee
                    this.sequence = 1
                },
                accountNumber = 8,
                signMode = SignMode.SIGN_MODE_LEGACY_AMINO_JSON
            )

            // z/8awwXiFSObxVIHMeIDDy7NRiHPzLYIwAx4AMcehfFZ/VyxlYrILxeXSuSmiO+WacT/C6ApBsoxtzvRzV7O6gA=
//oVN7JefL5gxLzqO8UOY9R7HjqtQfJxvYLBH955NGG6Ar3JntX8IxbuPnFa6fmEqAJ/NBCyXkBnyd3g31t2J6VgA=
                assertEquals("z/8awwXiFSObxVIHMeIDDy7NRiHPzLYIwAx4AMcehfFZ/VyxlYrILxeXSuSmiO+WacT/C6ApBsoxtzvRzV7O6gA=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
            createTx.getSignatures(0)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testJsonToAny() {
        val strJson = """
            {
            "fee": {
                "amount": [
                    {
                        "amount": "32254320000000000",
                        "denom": "axpla"
                    }
                ],
                "gas_limit": "115194"
            },
            "memo": "",
            "msgs": [
                {
                    "@type": "/cosmos.bank.v1beta1.MsgSend",
                    "amount": [
                        {
                            "amount": "10000000000000000",
                            "denom": "axpla"
                        }
                    ],
                    "from_address": "xpla1lg22287cj523vgdah8z4287nuzct43tmdtj69w",
                    "to_address": "xpla1m9ttxu9dewu9s7jyuzaz9przerq4x8ev3crsem"
                }
            ]
        }
        """.trimIndent()

        val builder = Tx.CreateTxOptions.newBuilder()
        JsonFormat.parser().usingTypeRegistry(TypeRegistrySingleton.typeRegistry).merge(strJson, builder)
        val createTx = builder.build()

        assertEquals(
            "0a1c2f636f736d6f732e62616e6b2e763162657461312e4d736753656e6412760a2b78706c61316c673232323837636a3532337667646168387a343238376e757a63743433746d64746a363977122b78706c61316d397474787539646577753973376a79757a617a3970727a657271347838657633637273656d1a1a0a056178706c6112113130303030303030303030303030303030",
            createTx.getMsgs(0).toByteArray().toHexString()
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testMsgExecuteJsonToAny() {
        val strJson = """
            {
          "fee": {"amount":[{"amount":"427803512500000000","denom":"axpla"}],"gas_limit":"503298"},
          "msgs": [
            {
              "@type": "/cosmwasm.wasm.v1.MsgExecuteContract",
              "contract": "xpla1vgay526xzh725vpur7drsyxhlvg4fxfvu5dczcctz35ct23q4vpqxqdemw",
              "funds": [
                {
                  "amount": "500000000000000000",
                  "denom": "axpla"
                }
              ],
              "msg": "eyJzd2FwIjogeyJiZWxpZWZfcHJpY2UiOiAiMTE0NC4xNjQ3NTk3MjU0MDA0NTc2NjUiLCAiZGVhZGxpbmUiOiAxNzQwNjQwMDUwLCAibWF4X3NwcmVhZCI6ICIwLjAwMTAiLCAib2ZmZXJfYXNzZXQiOiB7ImFtb3VudCI6ICI1MDAwMDAwMDAwMDAwMDAwMDAwIiwgImluZm8iOiB7Im5hdGl2ZV90b2tlbiI6IHsiZGVub20iOiAiYXhwbGEifX19fX0=",
              "sender": "xpla1lg22287cj523vgdah8z4287nuzct43tmdtj69w"
            }
          ]
        }
        """.trimIndent()

        val builder = Tx.CreateTxOptions.newBuilder()
        JsonFormat.parser().usingTypeRegistry(TypeRegistrySingleton.typeRegistry).merge(strJson, builder)
        val createTx = builder.build()

        assertEquals(
            "0a242f636f736d7761736d2e7761736d2e76312e4d736745786563757465436f6e747261637412d3020a2b78706c61316c673232323837636a3532337667646168387a343238376e757a63743433746d64746a363977123f78706c613176676179353236787a6837323576707572376472737978686c76673466786676753564637a6363747a3335637432337134767071787164656d771ac5017b2273776170223a207b2262656c6965665f7072696365223a2022313134342e313634373539373235343030343537363635222c2022646561646c696e65223a20313734303634303035302c20226d61785f737072656164223a2022302e30303130222c20226f666665725f6173736574223a207b22616d6f756e74223a202235303030303030303030303030303030303030222c2022696e666f223a207b226e61746976655f746f6b656e223a207b2264656e6f6d223a20226178706c61227d7d7d7d7d2a1b0a056178706c611212353030303030303030303030303030303030",
            createTx.getMsgs(0).toByteArray().toHexString()
        )
    }
}

