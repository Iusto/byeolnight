package com.byeolnight.controller;

import com.byeolnight.application.post.GalaxyPostService;
import com.byeolnight.domain.entity.post.Post;
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
    public List<Post> getAllPosts() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Post getPost(@PathVariable Long id) {
        return service.findById(id).orElseThrow();
    }

    @PostMapping
    public Post createPost(@RequestBody Post post) {
        return service.save(post);
    }

    @DeleteMapping("/{id}")
    public void deletePost(@PathVariable Long id) {
        service.delete(id);
    }
}
