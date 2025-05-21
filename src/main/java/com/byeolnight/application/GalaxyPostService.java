package com.byeolnight.application;

import com.byeolnight.domain.GalaxyPost;
import com.byeolnight.infrastructure.GalaxyPostRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GalaxyPostService {

    private final GalaxyPostRepository repository;

    public GalaxyPostService(GalaxyPostRepository repository) {
        this.repository = repository;
    }

    public List<GalaxyPost> findAll() {
        return repository.findAll();
    }

    public Optional<GalaxyPost> findById(Long id) {
        return repository.findById(id);
    }

    public GalaxyPost save(GalaxyPost post) {
        return repository.save(post);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
