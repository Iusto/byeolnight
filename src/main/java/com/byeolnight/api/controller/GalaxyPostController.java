package com.byeolnight.api.controller;

import com.byeolnight.domain.GalaxyPost;
import com.byeolnight.application.GalaxyPostService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/galaxy")
public class GalaxyPostController {

    private final GalaxyPostService service;

    public GalaxyPostController(GalaxyPostService service) {
        this.service = service;
    }

    @GetMapping
    public List<GalaxyPost> getAllPosts() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public GalaxyPost getPost(@PathVariable Long id) {
        return service.findById(id).orElseThrow();
    }

    @PostMapping
    public GalaxyPost createPost(@RequestBody GalaxyPost post) {
        return service.save(post);
    }

    @DeleteMapping("/{id}")
    public void deletePost(@PathVariable Long id) {
        service.delete(id);
    }
}
