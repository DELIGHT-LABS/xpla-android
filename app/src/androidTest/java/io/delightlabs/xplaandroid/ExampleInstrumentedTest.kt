package io.delightlabs.xplaandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.google.protobuf.Any
import cosmos.bank.v1beta1.msgSend
import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmos.tx.signing.v1beta1.Signing.SignMode
import cosmos.tx.v1beta1.TxOuterClass.Fee
import io.delightlabs.xplaandroid.api.APIRequester
import io.delightlabs.xplaandroid.api.APIReturn
import io.delightlabs.xplaandroid.api.HttpMethod
import io.delightlabs.xplaandroid.api.XplaNetwork
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith


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
        gasPrices = listOf()
    )

//    @Test
//    fun useAppContext() {
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("io.delightlabs.xplaandroid", appContext.packageName)
//    }

    @Test
    fun lcdWallet(){
        val seedPhrase = "table dinner sibling crisp hen genuine wing volume sport north omit cushion struggle script dinosaur merge medal visa also mixture faint surge boy wild"
        val wallet = lcd.wallet(seedPhrase)
        assertEquals("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9", wallet.address)
    }

    @Test
    fun lcdAuth() {
        val response: APIReturn.Account? = lcd.authAPI.accountInfo("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9")
        response?.baseAccount?.let { println("public address: ${it.address}") }
    }

    data class Query(val pairs: Pairs)
    data class Pairs(val limit: Int)
    @Test
    fun lcdWasm() {
        val query = Query(Pairs(20))
        val gson = Gson()
        val json = gson.toJson(query)
        val response: APIReturn.PairResponse?
            = lcd.wasmAPI.contractQuery(
            "xpla1j4kgjl6h4rt96uddtzdxdu39h0mhn4vrtydufdrk4uxxnrpsnw2qug2yx2",
            json.toString())
        response?.let {  println("wasm smartQuery:: ${it.data.pairs[0]}") }
    }

    @Test
    fun balanceAPI() {
        val response: APIReturn.BalanceReturn? = lcd.bankAPI.balance("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9")
        response?.let { println("balance response: $response") }
    }

    @Test
    fun networkTest(){
        val apiRequester = APIRequester(XplaNetwork.TestNet)
        val result = apiRequester.request<APIReturn.AccountReturn>(HttpMethod.GET, "/cosmos/auth/v1beta1/accounts/xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9")
        if (result != null) {
            println("result: $result")
        }
    }

    @Test
    fun testCreateTx() {
        val seedPhrase = "table dinner sibling crisp hen genuine wing volume sport north omit cushion struggle script dinosaur merge medal visa also mixture faint surge boy wild"
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

            val broadcastRes = lcd.txAPI.broadcast(createTx)
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
    fun testSignAmino() {
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
                    fee = Fee.newBuilder().addAmount(0, sendCoin).setGasLimit(200000).build(),
                    sequence = 1),
                accountNumber = 0,
                signMode = SignMode.SIGN_MODE_LEGACY_AMINO_JSON
            )

            assertEquals("MEgb9Kz2awtey5G9rC2ptBTYlRqUN+qfS1Qz27r3tdsmNr6qU+L2fMupYfUvTUNlT4GPN+7SL5nfWkGixPa39QE=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
            createTx.getSignatures(0)
        }
    }

    @Test
    fun testSignAminoWithPayer() {
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
                    fee = Fee.newBuilder().addAmount(0, sendCoin).setGasLimit(200000).setPayer("xpla1nns26tapuzt36vdz0aadk7svm8p6xndtmwlyg8").build(),
                    sequence = 2),
                accountNumber = 0,
                signMode = SignMode.SIGN_MODE_LEGACY_AMINO_JSON
            )

            assertEquals("I+F1N1QqNJ3MuWI5hGUaSyehWzPEVvFvOJZ/kZxMoZBMxVk8fkoTvB7m+qclhcspUiXzxtAu3By0+fDexCVOrAE=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
            createTx.getSignatures(0)
        }
    }

    @Test
    fun testSignAminoWithPayerWhenCosmos47() {
        val seedPhrase = "segment symbol pigeon tourist shop brush enter combine tornado pole snow federal lobster reopen drama wagon company salmon comfort rural palm fiscal crack roof"
        val lcd = LCDClient(
            XplaNetwork.TestNet,
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
                    fee = Fee.newBuilder().addAmount(0, sendCoin).setGasLimit(200000).setPayer("xpla1nns26tapuzt36vdz0aadk7svm8p6xndtmwlyg8").build(),
                    sequence = 2),
                accountNumber = 0,
                signMode = SignMode.SIGN_MODE_LEGACY_AMINO_JSON
            )

            assertEquals("1WW5WdDSn/7DAImtpZxgggTMcWoQrxqGHUGyQUpuCFcsMEgAl3Vxgteye4DSyxVzlM5imp3eRp/UJsPYgnn/5AE=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
            createTx.getSignatures(0)
        }
    }
}

