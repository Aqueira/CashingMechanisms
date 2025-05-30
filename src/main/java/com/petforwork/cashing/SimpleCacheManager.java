package com.petforwork.cashing;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.Collection;
import java.util.concurrent.*;

@Slf4j
public class SimpleCacheManager implements CacheManager {
    private final ConcurrentMap<String, SimpleCache> caches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final Long ttlTime;
    private final Long cleanupTime;


    public SimpleCacheManager(Long ttlTime, Long cleanupInterval) {
        this.ttlTime = ttlTime;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.cleanupTime = cleanupInterval;

        scheduler.scheduleAtFixedRate(
                this::cleanupExpiredCaches,
                cleanupInterval,
                cleanupInterval, TimeUnit.MINUTES
        );
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, value -> new SimpleCache(name, ttlTime, cleanupTime));
    }

    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }

    private void cleanupExpiredCaches() {
        log.info("Running expired caches cleanup");
        for (SimpleCache cache : caches.values()) {
            cache.cleanupExpiredEntries();
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(20, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    private static class SimpleCache implements Cache {
        private final String name;
        private final Long ttlTime;
        private final Long cleanupTime;
        private final ConcurrentHashMap<Object, EntryCache> storage = new ConcurrentHashMap<>();

        public SimpleCache(String name, Long ttlTime, Long cleanupTime) {
            this.name = name;
            this.ttlTime = ttlTime;
            this.cleanupTime = cleanupTime;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Object getNativeCache() {
            return storage;
        }

        @Override
        public ValueWrapper get(Object key) {
            EntryCache entry = storage.get(key);

            if(entry != null){
                if(entry.isExpired()){
                    evict(key);
                    return null;
                }
                return new SimpleValueWrapper(entry.getValue());
            }
            return null;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            EntryCache entry = storage.get(key);

            if(entry == null){
                return null;
            }

            if(type != null && !entry.isExpired() && type.isInstance(entry.getValue())) {
                return type.cast(entry.getValue());
            }
            return null;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
           EntryCache value = storage.compute(key, (keyy, valuee) -> {
               if(valuee == null || valuee.isExpired()){
                   try {
                       return new EntryCache(valueLoader.call(), ttlTime, cleanupTime);
                   } catch (Exception e) {
                       throw new ValueRetrievalException(key, valueLoader, e);
                   }
               }
               return valuee;
           });
           return (T) value.getValue();
        }

        @Override
        public void put(Object key, Object value) {
            storage.put(key, new EntryCache(value, ttlTime, cleanupTime));
        }

        @Override
        public void evict(Object key) {
            storage.remove(key);
        }

        @Override
        public void clear() {
            storage.clear();
        }

        public void cleanupExpiredEntries() {
            storage.entrySet().removeIf(e -> e.getValue().isAutoExpired());
            log.info("auto cleanup expired entries");
        }

        private record EntryCache(Object entryValue, Long expirationTime, Long autoExpireTime) {
            private final static Long MILLIS_PER_MINUTE = 60 * 1000L;

            private EntryCache(Object entryValue, Long expirationTime, Long autoExpireTime) {
                this.entryValue = entryValue;
                this.expirationTime = System.currentTimeMillis() + expirationTime * MILLIS_PER_MINUTE;
                this.autoExpireTime = autoExpireTime * MILLIS_PER_MINUTE;
            }

            public boolean isExpired() {
                return System.currentTimeMillis() > expirationTime;
            }

            public boolean isAutoExpired() {
                return System.currentTimeMillis() > autoExpireTime;
            }

            public Object getValue() {
                return entryValue;
            }
        }
    }
}
