package com.byeolnight.application;

import com.byeolnight.application.post.GalaxyPostService;
import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class GalaxyPostServiceTest {

    @Mock
    private PostRepository repository;

    @InjectMocks
    private GalaxyPostService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAll() {
        Post post1 = new Post();
        Post post2 = new Post();
        when(repository.findAll()).thenReturn(Arrays.asList(post1, post2));

        List<Post> result = service.findAll();

        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testFindById() {
        Post post = new Post();
        post.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(post));

        Optional<Post> result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void testSave() {
        Post post = new Post();
        when(repository.save(post)).thenReturn(post);

        Post result = service.save(post);

        assertNotNull(result);
        verify(repository, times(1)).save(post);
    }

    @Test
    void testDelete() {
        service.delete(1L);
        verify(repository, times(1)).deleteById(1L);
    }
}
