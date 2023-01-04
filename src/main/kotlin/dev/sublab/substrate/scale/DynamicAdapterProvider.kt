package dev.sublab.substrate.scale

import dev.sublab.common.numerics.*
import dev.sublab.scale.ScaleCodecAdapter
import dev.sublab.scale.ScaleCodecAdapterProvider
import dev.sublab.scale.adapters.*
import dev.sublab.substrate.metadata.RuntimeMetadata
import dev.sublab.substrate.metadata.lookup.type.RuntimeTypeDef
import dev.sublab.substrate.metadata.lookup.type.def.RuntimeTypeDefPrimitive
import dev.sublab.substrate.metadata.lookup.type.def.RuntimeTypeDefPrimitive.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation

internal class Adapter<T>(
    val scaleAdapter: ScaleCodecAdapter<T>,
    val toByteArray: ((T) -> ByteArray)? = null,
    val fromByteArray: ((ByteArray) -> T)? = null
)

internal class DynamicAdapterProvider(
    private val adapterResolver: ScaleCodecAdapterProvider,
    private val runtimeMetadata: Flow<RuntimeMetadata>
) {
    @Suppress("unused")
    suspend fun <T: Any> findAdapter(type: KClass<T>) = findAdapter<T>(type.createType())

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> findAdapter(type: KType): Adapter<T> {
        // Find annotation
        val kClass = type.classifier as? KClass<*> ?: throw InvalidTypeException(type)
        val annotation = kClass.findAnnotation<DynamicType>()
            ?: throw TypeIsNotDynamicException(type)

        // Find type
        val typeDef = runtimeMetadata.first().lookup.findItemByIndex(annotation.lookupIndex)?.def
            ?: throw TypeIsNotFoundInRuntimeMetadataException(type)

        return when (typeDef) {
            is RuntimeTypeDef.Array -> throw UnsupportedDynamicTypeException(type)
            is RuntimeTypeDef.BitSequence -> throw UnsupportedDynamicTypeException(type)
            is RuntimeTypeDef.Compact -> Adapter(
                BigIntegerAdapter(adapterResolver),
                { it.toByteArray() },
                { BigInteger(it) }
            )
            is RuntimeTypeDef.Composite -> throw UnsupportedDynamicTypeException(type)
            is RuntimeTypeDef.Primitive -> findAdapter(typeDef.primitive, type)
            is RuntimeTypeDef.Sequence -> throw UnsupportedDynamicTypeException(type)
            is RuntimeTypeDef.Tuple -> throw UnsupportedDynamicTypeException(type)
            is RuntimeTypeDef.Variant -> throw UnsupportedDynamicTypeException(type)
        } as Adapter<T>
    }

    private fun findAdapter(primitive: RuntimeTypeDefPrimitive, type: KType): Adapter<*> = when (primitive) {
        Bool -> Adapter(BooleanAdapter())
        RuntimeTypeDefPrimitive.Char -> throw UnsupportedDynamicTypeException(type)
        RuntimeTypeDefPrimitive.String -> Adapter(StringAdapter(adapterResolver))
        U8 -> Adapter(
            UInt8Adapter(),
            { it.toByteArray() },
            { it.toUInt8() }
        )
        U16 -> Adapter(
            UInt16Adapter(),
            { it.toByteArray() },
            { it.toUInt16() }
        )
        U32 -> Adapter(
            UInt32Adapter(),
            { it.toByteArray() },
            { it.toUInt32() }
        )
        U64 -> Adapter(
            UInt64Adapter(),
            { it.toByteArray() },
            { it.toUInt64() }
        )
        U128 -> Adapter(
            UInt128Adapter(),
            { it.toByteArray() },
            { it.toUInt128() }
        )
        U256 -> Adapter(
            UInt256Adapter(),
            { it.toByteArray() },
            { it.toUInt256() }
        )
        I8 -> Adapter(
            Int8Adapter(),
            { it.toByteArray() },
            { it.toInt8() }
        )
        I16 -> Adapter(
            Int16Adapter(),
            { it.toByteArray() },
            { it.toInt16() }
        )
        I32 -> Adapter(
            Int32Adapter(),
            { it.toByteArray() },
            { it.toInt32() }
        )
        I64 -> Adapter(
            Int64Adapter(),
            { it.toByteArray() },
            { it.toInt64() }
        )
        I128 -> Adapter(
            Int128Adapter(),
            { it.toByteArray() },
            { it.toInt128() }
        )
        I256 -> Adapter(
            Int256Adapter(),
            { it.toByteArray() },
            { it.toInt256() }
        )
    }
}