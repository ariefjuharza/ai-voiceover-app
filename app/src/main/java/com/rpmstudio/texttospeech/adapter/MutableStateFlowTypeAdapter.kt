package com.rpmstudio.texttospeech.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.reflect.Type

class MutableStateFlowTypeAdapter<T>(private val valueType: Class<T>) :
    JsonSerializer<MutableStateFlow<T>>,
    JsonDeserializer<MutableStateFlow<T>> {

    override fun serialize(
        src: MutableStateFlow<T>,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        // Serialize the current value using the context for proper type handling
        return context.serialize(src.value)
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): MutableStateFlow<T> {
        // Deserialize the value using the context and the provided valueType
        try {
            val value = context.deserialize<T>(json, valueType)
            return MutableStateFlow(value)
        } catch (e: JsonParseException) {
            // Handle deserialization errors
            throw JsonParseException("Error deserializing MutableStateFlow value", e)
        }
    }
}

