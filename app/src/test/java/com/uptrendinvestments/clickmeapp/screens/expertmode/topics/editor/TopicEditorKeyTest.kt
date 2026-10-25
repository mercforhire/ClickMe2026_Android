package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor

import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.testing.topicItem
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicEditorKeyTest {
    @Test
    fun forTopicCarriesThePaidItemFields() {
        val item =
            topicItem(
                id = "t9",
                title = "Tax",
                amount = 1250,
                currency = "CAD",
                durationMins = 90,
                iconSlug = "finance",
                description = "D",
                tags = listOf(ExpertiseTagItem("tax", "Tax")),
            )
        val seed = TopicEditorKey.forTopic(item).topic!!
        assertEquals("t9", seed.id)
        assertEquals("Tax", seed.title)
        assertEquals("D", seed.description)
        assertEquals(90, seed.durationMins)
        assertEquals(1250L, seed.amountMinor)
        assertFalse(seed.isFree)
        assertEquals("CAD", seed.currency)
        assertEquals("finance", seed.iconSlug)
        assertEquals(listOf(TopicEditorSeedTag("tax", "Tax")), seed.tags)
    }

    @Test
    fun aFreeItemWithABlankCurrencyCarriesNull() {
        val seed = TopicEditorKey.forTopic(topicItem(isFree = true)).topic!!
        assertTrue(seed.isFree)
        assertNull(seed.currency)
    }

    @Test
    fun theKeyRoundTripsThroughItsSerializer() {
        val key =
            TopicEditorKey.forTopic(
                topicItem(tags = listOf(ExpertiseTagItem("tax", "Tax"), ExpertiseTagItem("law", "Law"))),
            )
        assertEquals(key, roundTrip(TopicEditorKey.serializer(), key))
    }

    @Test
    fun newTopicHasNoSeed() {
        assertNull(TopicEditorKey.newTopic().topic)
        assertEquals(TopicEditorKey.newTopic(), roundTrip(TopicEditorKey.serializer(), TopicEditorKey.newTopic()))
    }
}

/**
 * Round trip through the generated serializer using a minimal positional format, because the JSON format artifact is
 * not on the unit-test compile classpath. It exercises exactly what the nav back stack needs: every field of the key
 * encodes and decodes, including nulls and the nested list.
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
