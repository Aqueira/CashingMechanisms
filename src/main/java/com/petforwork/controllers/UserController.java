package com.petforwork.controllers;

import com.petforwork.entities.User;
import com.petforwork.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/user/api/")
public class UserController {
    private final UserService userService;

    //PathVariable если надо, get{id}, @PathVariable("id") Long id
    @GetMapping("get")
    public ResponseEntity<User> getUser(@RequestParam("id") Long id)  {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping("create")
    public ResponseEntity<User> create(@RequestBody  User user) {
        return ResponseEntity.ok(userService.create(user));
    }

    @PatchMapping("update")
    public ResponseEntity<User> update(@RequestBody User user) {
        return ResponseEntity.ok(userService.update(user));
    }

    @DeleteMapping("delete")
    public ResponseEntity<?> delete(@RequestParam Long id) {
        return ResponseEntity.ok(userService.delete(id));
    }
}
