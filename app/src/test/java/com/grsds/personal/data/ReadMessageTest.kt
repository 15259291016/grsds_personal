package com.grsds.personal.data

import com.grsds.personal.MessageCenter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 消息未读数徽标的数据层：已读集合编解码 + 未读数口径。
 * 徽标数字完全由此推导，不写死常量。
 */
class ReadMessageTest {

    @Test
    fun `encode then decode read ids keeps order and content`() {
        val ids = listOf("msg-annual", "msg-security", "msg-maintain")
        val raw = AppPreferencesCodec.encodeReadMessageIds(ids)
        assertEquals(ids, AppPreferencesCodec.decodeReadMessageIds(raw))
    }

    @Test
    fun `decode empty or malformed raw returns empty list`() {
        assertTrue(AppPreferencesCodec.decodeReadMessageIds(null).isEmpty())
        assertTrue(AppPreferencesCodec.decodeReadMessageIds("").isEmpty())
        assertTrue(AppPreferencesCodec.decodeReadMessageIds("   ").isEmpty())
        assertTrue(AppPreferencesCodec.decodeReadMessageIds("not-json").isEmpty())
        assertTrue(AppPreferencesCodec.decodeReadMessageIds("[1,2,3]").isEmpty())
    }

    @Test
    fun `empty raw encodes to empty list`() {
        val raw = AppPreferencesCodec.encodeReadMessageIds(emptyList())
        assertTrue(AppPreferencesCodec.decodeReadMessageIds(raw).isEmpty())
    }

    @Test
    fun `unread count equals total when nothing read`() {
        assertEquals(MessageCenter.items.size, MessageCenter.unreadCount(emptyList()))
    }

    @Test
    fun `unread count drops by one per read id`() {
        val total = MessageCenter.items.size
        val first = MessageCenter.items.first().id
        assertEquals(total - 1, MessageCenter.unreadCount(listOf(first)))
        val second = MessageCenter.items[1].id
        assertEquals(total - 2, MessageCenter.unreadCount(listOf(first, second)))
    }

    @Test
    fun `unread count is zero when all messages read`() {
        assertEquals(0, MessageCenter.unreadCount(MessageCenter.allIds()))
    }

    @Test
    fun `stale or unknown read ids do not affect unread count`() {
        val total = MessageCenter.items.size
        assertEquals(total, MessageCenter.unreadCount(listOf("deleted-msg", "legacy-id")))
        assertEquals(
            total - 1,
            MessageCenter.unreadCount(listOf(MessageCenter.items.first().id, "deleted-msg"))
        )
    }

    @Test
    fun `all ids covers every message exactly once`() {
        val ids = MessageCenter.allIds()
        assertEquals(MessageCenter.items.size, ids.size)
        assertEquals(ids.toSet().size, ids.size)
        assertTrue(MessageCenter.items.all { it.id in ids })
    }
}
