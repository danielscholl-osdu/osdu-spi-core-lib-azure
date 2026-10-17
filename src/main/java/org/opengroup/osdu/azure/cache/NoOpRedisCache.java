// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.azure.cache;

import io.lettuce.core.codec.RedisCodec;
import org.opengroup.osdu.azure.logging.CoreLoggerFactory;
import org.opengroup.osdu.core.common.cache.IRedisCache;

/**
 * Redis cache client that does nothing!
 * @param <V> valueClass
 */
public final class NoOpRedisCache<V> implements IRedisCache<String, V> {

    private static final String LOGGER_NAME = NoOpRedisCache.class.getName();

    @Override
    public void put(final String k, final V o) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - put");
    }

    @Override
    public V get(final String k) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - get");
        return null;
    }

    @Override
    public void delete(final String k) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - delete");
    }

    @Override
    public void clearAll() {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - clearAll");
    }

    @Override
    public void put(final String k, final long l, final V o) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - put with ttl");
    }

    @Override
    public boolean updateTtl(final String k, final long l) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - updateTtl");
        return false;
    }

    @Override
    public Long getTtl(final String k) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - getTtl");
        return 0L;
    }

    @Override
    public String info() {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - info");
        return null;
    }

    @Override
    public Long increment(final String k) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - increment");
        return null;
    }

    @Override
    public Long incrementBy(final String k, final long l) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - incrementBy");
        return null;
    }

    @Override
    public Long decrement(final String k) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - decrement");
        return null;
    }

    @Override
    public Long decrementBy(final String k, final long l) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - decrementBy");
        return null;
    }

    @Override
    public RedisCodec<String, V> getCodec(final Class<String> aClass, final Class<V> aClass1) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - getCodec");
        return null;
    }

    @Override
    public boolean authenticate(final String username, final String password) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - authenticate");
        return true;
    }

    @Override
    public long deleteByPattern(final String pattern) {
        CoreLoggerFactory.getInstance().getLogger(LOGGER_NAME).info("NoOpRedisCache - deleteByPattern");
        return 0L;
    }
}
