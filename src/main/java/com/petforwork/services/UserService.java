package com.petforwork.services;


import com.petforwork.entities.User;
import com.petforwork.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public User create(User user){
        return userRepository.save(user);
    }

    @CachePut(cacheManager = "customCache", value = "users", key = "#user.id")
    public User update(User user){
        return userRepository.save(user);
    }

    @CacheEvict(cacheManager = "customCache", value = "users", key = "#id")
    public User delete(Long id){
        return userRepository.findById(id)
                .map(user -> {
                    userRepository.delete(user);
                    return user;
                })
                .orElseThrow(()->new RuntimeException("User not found"));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheManager = "customCache", value = "users", key = "#id")
    public User findById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
