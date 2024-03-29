package ua.eco.aggregator.api.public.plugins

fun <T> cached(maxAgeMillis: Long, initializer: suspend () -> T): Cached<T> = CachedImpl(maxAgeMillis, initializer)

/**
 * Represents a cached value.
 *
 * To create an instance of [Cached] use the [cached] function.
 */
interface Cached<out T> {
    /**
     * Gets the cached value of the current Cached instance.
     * If the value is not up-to-date, calls initializer and updates stored value.
     */
    suspend fun getValue(): T

    /**
     * Returns `true` if a value for this Cached instance is up-to-date, and `false` otherwise.
     */
    fun isUpToDate(): Boolean
}

internal object UNINITIALIZED_VALUE

internal class CachedImpl<out T>(private val maxAgeMillis: Long, private val initializer: suspend () -> T) : Cached<T> {
    private var _value: Any? = UNINITIALIZED_VALUE
    private var updatedAt: Long? = null

    override suspend fun getValue(): T {
        if (_value === UNINITIALIZED_VALUE) {
            _value = initializer()
            updatedAt = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - updatedAt!! >= maxAgeMillis) {
            _value = initializer()
            updatedAt = System.currentTimeMillis()
        }
        @Suppress("UNCHECKED_CAST")
        return _value as T
    }

    override fun isUpToDate(): Boolean {
        return if (updatedAt == null) {
            false
        } else {
            System.currentTimeMillis() - updatedAt!! >= maxAgeMillis
        }
    }

    override fun toString(): String =
        if (_value != UNINITIALIZED_VALUE) _value.toString() else "Cached value not initialized yet."
}