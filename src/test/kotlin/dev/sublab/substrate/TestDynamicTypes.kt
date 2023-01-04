package dev.sublab.substrate

import dev.sublab.common.FromByteArray
import dev.sublab.hex.hex
import dev.sublab.scale.default.DefaultScaleCodecAdapterProvider
import dev.sublab.scale.read
import dev.sublab.scale.write
import dev.sublab.substrate.scale.Balance
import dev.sublab.substrate.scale.DynamicAdapter
import dev.sublab.substrate.scale.DynamicAdapterProvider
import dev.sublab.substrate.scale.Index
import dev.sublab.substrate.support.KusamaNetwork
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.test.Test

internal class TestDynamicTypes {
    private val network = KusamaNetwork()
    private val client = SubstrateClient(url = network.rpcUrl)

    private val adapterProvider = DefaultScaleCodecAdapterProvider()
    private val dynamicAdapterProvider = DynamicAdapterProvider(adapterProvider, client.getRuntime())

    private fun <T: FromByteArray> makeDynamicAdapter(type: KClass<T>) = DynamicAdapter<T>(dynamicAdapterProvider)

    @Test
    fun testAdapter() = runBlocking {
        val index = Index(1.toBigInteger())
        test(index, Index::class) { lhs, rhs ->
            if (lhs.value != rhs.value) println("Expected: ${lhs.value}, received: {${rhs.value}")
            lhs.value == rhs.value
        }

        val balance = Balance(100.toBigInteger())
        test(balance, Balance::class) { lhs, rhs ->
            if (lhs.value != rhs.value) println("Expected: ${lhs.value}, received: {${rhs.value}")
            lhs.value == rhs.value
        }
    }

    private suspend fun <T: FromByteArray> test(value: T, type: KClass<T>, equals: (T, T) -> Boolean) {
        val adapter = makeDynamicAdapter(type)
        val encoded = adapter.write(value, type)
        println("Encoded ${value.toByteArray().hex.encode(true)} to ${encoded.hex.encode(true)}")
        val decoded = adapter.read(encoded, type)
        println("Decoded to ${decoded.toByteArray().hex.encode(true)}")
        assert(equals(value, decoded))
    }
}