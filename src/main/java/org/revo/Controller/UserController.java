package org.revo.Controller;

import org.revo.Domain.Ids;
import org.revo.Domain.User;
import org.revo.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("api")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/user/{id}")
    public Optional<User> user(@PathVariable("id") String id) {
        return userService.findOne(id);
    }

    @PostMapping("/user")
    public Iterable<User> user(@RequestBody Ids ids) {
        return userService.findAll(ids.getIds());
    }
}