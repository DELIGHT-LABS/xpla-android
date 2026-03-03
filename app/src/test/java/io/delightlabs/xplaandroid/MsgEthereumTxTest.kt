package io.delightlabs.xplaandroid

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import org.junit.Assert.*
import org.junit.Test

class MsgEthereumTxTest {

    /**
     * TypeRegistry에 cosmos/evm MsgEthereumTx가 정상 등록되었는지 확인
     */
    @Test
    fun testTypeRegistryContainsNewMsgEthereumTx() {
        val registry = TypeRegistrySingleton.typeRegistry

        // 새 cosmos/evm MsgEthereumTx type URL로 찾을 수 있어야 함
        val descriptor = registry.find(
            registry.getDescriptorForTypeUrl("type.googleapis.com/cosmos.evm.vm.v1.MsgEthereumTx")
                .fullName
        )
        assertNotNull("cosmos.evm.vm.v1.MsgEthereumTx should be registered", descriptor)
    }

    /**
     * MsgEthereumTx 형태로 오는 JSON 응답을 정상 처리할 수 있는지 확인
     */
    @Test
    fun testNewMsgEthereumTxJsonConversion() {
        val fromBytes = ByteString.copyFrom(
            byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        )
        val rawBytes = ByteString.copyFrom(
            byteArrayOf(0xf8.toByte(), 0x65.toByte())
        )

        val msg = cosmos.evm.vm.v1.Tx.MsgEthereumTx.newBuilder()
            .setFrom(fromBytes)
            .setRaw(rawBytes)
            .build()

        // Any로 감싸기
        val anyMsg = Any.newBuilder()
            .setTypeUrl("/cosmos.evm.vm.v1.MsgEthereumTx")
            .setValue(msg.toByteString())
            .build()

        // JSON 출력 (TypeRegistry 사용)
        val jsonPrinter = JsonFormat.printer()
            .usingTypeRegistry(TypeRegistrySingleton.typeRegistry)

        val json = jsonPrinter.print(anyMsg)
        assertTrue("JSON should contain type URL", json.contains("cosmos.evm.vm.v1.MsgEthereumTx"))
        assertTrue("JSON should contain from field", json.contains("from"))

        // JSON -> Any 역변환
        val builder = Any.newBuilder()
        JsonFormat.parser()
            .usingTypeRegistry(TypeRegistrySingleton.typeRegistry)
            .merge(json, builder)
        val restored = builder.build()

        assertEquals("/cosmos.evm.vm.v1.MsgEthereumTx", restored.typeUrl)
    }

    /**
     * 기존 ethermint MsgEthereumTx도 여전히 TypeRegistry에서 동작하는지 확인
     */
    @Test
    fun testOldEthermintMsgEthereumTxStillWorks() {
        val registry = TypeRegistrySingleton.typeRegistry

        val descriptor = registry.find(
            registry.getDescriptorForTypeUrl("type.googleapis.com/ethermint.evm.v1.MsgEthereumTx")
                .fullName
        )
        assertNotNull("ethermint.evm.v1.MsgEthereumTx should still be registered", descriptor)
    }

    /**
     * Cube 테스트넷 실제 EVM TX 응답 JSON 파싱 테스트
     * TX: 276349708750F9737473DF8B8CA6622D79A01B8E14443A8873398C89E8849CEF
     */
    @Test
    fun testParseCubeEvmTxResponse() {
        val msgJson = """
            {
                "@type": "/cosmos.evm.vm.v1.MsgEthereumTx",
                "from": "5pRRwCc428tPXHPIFaCWvZER7EQ=",
                "raw": "0x02f8772f8204b88541314cf0008541314cf000830186a094e69451c02738dbcb4f5c73c815a096bd9111ec44880de0b6b3a764000080c080a0f12b1b7b8d7056139083b475405f3a153b3b83033f51b3f13570c0a29a2213f5a04303c4660648e3e14d25eda0ed724857fbcd4b5e84a69e7db511680208b1611d"
            }
        """.trimIndent()

        // JSON -> Any 파싱
        val builder = Any.newBuilder()
        JsonFormat.parser()
            .usingTypeRegistry(TypeRegistrySingleton.typeRegistry)
            .merge(msgJson, builder)
        val anyMsg = builder.build()

        assertEquals("/cosmos.evm.vm.v1.MsgEthereumTx", anyMsg.typeUrl)

        // Any -> MsgEthereumTx 역직렬화
        val msg = cosmos.evm.vm.v1.Tx.MsgEthereumTx.parseFrom(anyMsg.value)
        assertFalse("from should not be empty", msg.from.isEmpty)
        assertFalse("raw should not be empty", msg.raw.isEmpty)
    }
}