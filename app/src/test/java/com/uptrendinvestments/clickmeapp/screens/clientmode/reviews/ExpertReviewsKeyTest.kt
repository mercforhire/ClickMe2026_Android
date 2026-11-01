package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.FavoritesKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ExpertReviewsKeyTest {
    private fun profileKey(origin: StackOrigin) = ExpertProfileKey("e1", "Ada", "Engineer", null, origin = origin)

    @Test
    fun reviewsKeyCopiesIdNameAndOrigin() {
        StackOrigin.entries.forEach { origin ->
            assertEquals(
                ExpertReviewsKey("e1", "Ada Lovelace", origin),
                profileKey(origin).reviewsKey("Ada Lovelace"),
            )
        }
    }

    @Test
    fun sameExpertOnTwoStacksGivesDistinctKeys() {
        fun key(origin: StackOrigin) = profileKey(origin).reviewsKey("Ada")

        assertNotEquals(key(StackOrigin.Default), key(StackOrigin.Profile))
        assertNotEquals(key(StackOrigin.Search), key(StackOrigin.Chats))
    }

    @Test
    fun defaultOriginIsDefault() {
        assertEquals(StackOrigin.Default, ExpertReviewsKey("e1", "Ada").origin)
    }

    @Test
    fun keysRoundTripThroughSerialization() {
        val key = ExpertReviewsKey("e1", "Ada", StackOrigin.Profile)
        assertEquals(key, roundTrip(ExpertReviewsKey.serializer(), key))
        assertEquals(FavoritesKey, roundTrip(FavoritesKey.serializer(), FavoritesKey))
    }
}

/**
 * Round trip through the generated serializer using a minimal positional format, because the JSON format artifact is
 * not on the unit-test compile classpath (same approach as `ExpertNavigationTest`).
 */
@OptIn(ExperimentalSerializationApi::class)
private fun <T> roundTrip(
    serializer: KSerializer<T>,
    value: T,
): T {
    val values = ArrayDeque<Any?>()
    serializer.serialize(ValueEncoder(values), value)
    return serializer.deserialize(ValueDecoder(values))
}

@OptIn(ExperimentalSerializationApi::class)
private class ValueEncoder(
    private val out: ArrayDeque<Any?>,
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun encodeValue(value: Any) {
        out.addLast(value)
    }

    override fun encodeNull() {
        out.addLast(false)
    }

    override fun encodeNotNullMark() {
        out.addLast(true)
    }

    override fun beginCollection(
        descriptor: SerialDescriptor,
        collectionSize: Int,
    ): CompositeEncoder {
        out.addLast(collectionSize)
        return this
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class ValueDecoder(
    private val input: ArrayDeque<Any?>,
) : AbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeValue(): Any = input.removeFirst()!!

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = CompositeDecoder.DECODE_DONE

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = decodeInt()

    override fun decodeNotNullMark(): Boolean = decodeBoolean()
}
