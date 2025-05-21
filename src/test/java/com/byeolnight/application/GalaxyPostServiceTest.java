package com.byeolnight.application;

import com.byeolnight.domain.GalaxyPost;
import com.byeolnight.infrastructure.GalaxyPostRepository;
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
    private GalaxyPostRepository repository;

    @InjectMocks
    private GalaxyPostService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAll() {
        GalaxyPost post1 = new GalaxyPost();
        GalaxyPost post2 = new GalaxyPost();
        when(repository.findAll()).thenReturn(Arrays.asList(post1, post2));

        List<GalaxyPost> result = service.findAll();

        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testFindById() {
        GalaxyPost post = new GalaxyPost();
        post.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(post));

        Optional<GalaxyPost> result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void testSave() {
        GalaxyPost post = new GalaxyPost();
        when(repository.save(post)).thenReturn(post);

        GalaxyPost result = service.save(post);

        assertNotNull(result);
        verify(repository, times(1)).save(post);
    }

    @Test
    void testDelete() {
        service.delete(1L);
        verify(repository, times(1)).deleteById(1L);
    }
}
