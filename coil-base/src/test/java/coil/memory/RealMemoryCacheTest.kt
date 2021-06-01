package coil.memory

import coil.memory.MemoryCache.Key
import coil.memory.MemoryCache.Value
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.allocationByteCountCompat
import coil.util.createBitmap
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RealMemoryCacheTest {

    private lateinit var weakCache: RealWeakMemoryCache
    private lateinit var strongCache: RealStrongMemoryCache
    private lateinit var cache: MemoryCache

    @Before
    fun before() {
        weakCache = RealWeakMemoryCache()
        strongCache = RealStrongMemoryCache(Int.MAX_VALUE, weakCache)
        cache = RealMemoryCache(strongCache, weakCache)
    }

    @Test
    fun `can retrieve strong cached value`() {
        val key = Key("strong")
        val bitmap = createBitmap()

        assertNull(cache[key])

        strongCache.set(key, bitmap, false)

        assertEquals(bitmap, cache[key]?.bitmap)
    }

    @Test
    fun `can retrieve weak cached value`() {
        val key = Key("weak")
        val bitmap = createBitmap()

        assertNull(cache[key])

        weakCache.set(key, bitmap, false, bitmap.allocationByteCountCompat)

        assertEquals(bitmap, cache[key]?.bitmap)
    }

    @Test
    fun `remove removes from both caches`() {
        val key = Key("key")
        val bitmap = createBitmap()

        assertNull(cache[key])

        strongCache.set(key, bitmap, false)
        weakCache.set(key, bitmap, false, bitmap.allocationByteCountCompat)

        assertTrue(cache.remove(key))
        assertNull(strongCache.get(key))
        assertNull(weakCache.get(key))
    }

    @Test
    fun `clear clears all values`() {
        assertEquals(0, cache.size)

        strongCache.set(Key("a"), createBitmap(), false)
        strongCache.set(Key("b"), createBitmap(), false)
        weakCache.set(Key("c"), createBitmap(), false, 100)
        weakCache.set(Key("d"), createBitmap(), false, 100)

        assertEquals(2 * DEFAULT_BITMAP_SIZE, cache.size)

        cache.clear()

        assertEquals(0, cache.size)
        assertNull(cache[Key("a")])
        assertNull(cache[Key("b")])
        assertNull(cache[Key("c")])
        assertNull(cache[Key("d")])
    }

    @Test
    fun `set can be retrieved with get`() {
        val key = Key("a")
        val bitmap = createBitmap()
        cache[key] = Value(bitmap)

        assertEquals(bitmap, cache[key]?.bitmap)
    }

    @Test
    fun `set replaces strong and weak values`() {
        val key = Key("a")
        val bitmap = createBitmap()

        strongCache.set(key, createBitmap(), false)
        weakCache.set(key, createBitmap(), false, 100)
        cache[key] = Value(bitmap)

        assertEquals(bitmap, strongCache.get(key)?.bitmap)
        assertNull(weakCache.get(key))
    }

    @Test
    fun `setting the same bitmap multiple times can only be removed once`() {
        val key = Key("a")
        val bitmap = createBitmap()

        weakCache.set(key, bitmap, false, 100)
        weakCache.set(key, bitmap, false, 100)

        assertTrue(weakCache.remove(key))
        assertFalse(weakCache.remove(key))
    }
}
