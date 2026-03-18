package api.grpc.mapper

import com.google.protobuf.ListValue
import com.google.protobuf.NullValue
import com.google.protobuf.Struct
import com.google.protobuf.Value
import com.nekgamebling.game.v1.AggregatorDto
import com.nekgamebling.game.v1.aggregatorDto
import domain.model.Aggregator

object AggregatorProtoMapper {

    fun Aggregator.toProto(): AggregatorDto = aggregatorDto {
        identity = this@toProto.identity.value
        integration = this@toProto.integration
        config = this@toProto.config.toProtoStruct()
        active = this@toProto.active
    }

    fun Map<String, Any>.toProtoStruct(): Struct {
        val builder = Struct.newBuilder()
        for ((key, value) in this) {
            builder.putFields(key, toProtoValue(value))
        }
        return builder.build()
    }

    fun Struct.toDomainMap(): Map<String, Any> {
        return fieldsMap.mapValues { (_, value) -> fromProtoValue(value) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun toProtoValue(value: Any?): Value {
        val builder = Value.newBuilder()
        when (value) {
            null -> builder.nullValue = NullValue.NULL_VALUE
            is String -> builder.stringValue = value
            is Number -> builder.numberValue = value.toDouble()
            is Boolean -> builder.boolValue = value
            is Map<*, *> -> builder.structValue = (value as Map<String, Any>).toProtoStruct()
            is List<*> -> builder.listValue = ListValue.newBuilder()
                .addAllValues(value.map { toProtoValue(it) })
                .build()
            else -> builder.stringValue = value.toString()
        }
        return builder.build()
    }

    private fun fromProtoValue(value: Value): Any {
        return when (value.kindCase) {
            Value.KindCase.STRING_VALUE -> value.stringValue
            Value.KindCase.NUMBER_VALUE -> value.numberValue
            Value.KindCase.BOOL_VALUE -> value.boolValue
            Value.KindCase.STRUCT_VALUE -> value.structValue.toDomainMap()
            Value.KindCase.LIST_VALUE -> value.listValue.valuesList.map { fromProtoValue(it) }
            Value.KindCase.NULL_VALUE, Value.KindCase.KIND_NOT_SET, null -> ""
        }
    }
}
