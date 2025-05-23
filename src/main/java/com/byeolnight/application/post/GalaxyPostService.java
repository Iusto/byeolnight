package com.byeolnight.application.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GalaxyPostService {

    private final PostRepository repository;

    public GalaxyPostService(PostRepository repository) {
        this.repository = repository;
    }

    public List<Post> findAll() {
        return repository.findAll();
    }

    public Optional<Post> findById(Long id) {
        return repository.findById(id);
    }

    public Post save(Post post) {
        return repository.save(post);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
