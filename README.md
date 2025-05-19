
# SimpleCacheManager

`SimpleCacheManager` is a lightweight implementation of Spring's `CacheManager` interface, providing basic in-memory caching functionality with TTL support and automatic cleanup of expired entries.

## Features

- ✅ TTL (Time-To-Live) support per entry
- ✅ Automatic cleanup of expired entries via background scheduler
- ✅ Thread-safe with `ConcurrentHashMap`
- ✅ Compatible with Spring Cache API
- ❌ No distribution (local cache only)

## Usage

### Create an instance

```java
Long ttlMinutes = 10L;
Long cleanupIntervalMinutes = 5L;

SimpleCacheManager cacheManager = new SimpleCacheManager(ttlMinutes, cleanupIntervalMinutes);
```

### Get a cache

```java
Cache myCache = cacheManager.getCache("exampleCache");
```

### Work with cache

```java
// Put value
myCache.put("key", "value");

// Get value
Object value = myCache.get("key").get();

// Remove key
myCache.evict("key");

// Clear cache
myCache.clear();
```

## Expired Entry Cleanup

A scheduled task runs at the specified `cleanupIntervalMinutes`, removing entries that have exceeded their auto-expire time.

## Shutdown Procedure

Ensure proper shutdown of the background scheduler on application exit:

```java
@PreDestroy
public void shutdown() {
    scheduler.shutdown();
    ...
}
```

## Spring Configuration Example

```java
@Bean
public CacheManager cacheManager() {
    return new SimpleCacheManager(10L, 5L);
}
```

## Comparison with Other Cache Implementations

| Cache Type             | Storage Type       | TTL / Expiration      | Distributed Support        | Thread-Safe        | When to Use                                           |
|------------------------|--------------------|------------------------|-----------------------------|--------------------|--------------------------------------------------------|
| **SimpleCacheManager** | In-memory           | ✅ (TTL + auto-expire) | ❌                          | ✅                 | Simple standalone apps, tests, no external deps         |
| **Spring Default (ConcurrentMapCache)** | In-memory | ❌ (no TTL)           | ❌                          | ✅                 | For simple usage, temporary cache                      |
| **Caffeine**           | In-memory (LRU, etc)| ✅ (flexible policies) | ❌                          | ✅                 | High-performance caching with fine-tuning              |
| **Redis**              | Remote / In-memory  | ✅                     | ✅                          | ✅ (client-level)  | Distributed systems, microservices, persistent cache   |

### Recommendation Summary

- ✅ Use `SimpleCacheManager` for custom local caching without extra dependencies.
- ✅ Use `Caffeine` when performance and memory control are critical.
- ✅ Use `Redis` when you need distributed cache across services.

## License

MIT or any license suitable for your project.
