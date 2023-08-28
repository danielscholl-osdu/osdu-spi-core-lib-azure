package org.opengroup.osdu.azure.cache;

import com.lambdaworks.redis.codec.RedisCodec;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.core.common.cache.IRedisCache;

/**
 * Redis cache client that does nothing!
 * @param <K> keyClass
 * @param <V> valueClass
 */
public final class NoOpRedisCache<K, V> implements IRedisCache<K, V> {

    private static final String LOGGER_NAME = NoOpRedisCache.class.getName();

    @Override
    public void put(final K k, final V o) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - put");
    }

    @Override
    public V get(final K k) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - get");
        return null;
    }

    @Override
    public void delete(final K k) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - delete");
    }

    @Override
    public void clearAll() {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - clearAll");
    }

    @Override
    public void put(final K k, final long l, final V o) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - put with ttl");
    }

    @Override
    public boolean updateTtl(final K k, final long l) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - updateTtl");
        return false;
    }

    @Override
    public Long getTtl(final K k) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - getTtl");
        return 0L;
    }

    @Override
    public String info() {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - info");
        return null;
    }

    @Override
    public Long increment(final K k) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - increment");
        return null;
    }

    @Override
    public Long incrementBy(final K k, final long l) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - incrementBy");
        return null;
    }

    @Override
    public Long decrement(final K k) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - decrement");
        return null;
    }

    @Override
    public Long decrementBy(final K k, final long l) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - decrementBy");
        return null;
    }

    @Override
    public RedisCodec<K, V> getCodec(final Class<K> aClass, final Class<V> aClass1) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - getCodec");
        return null;
    }
}
