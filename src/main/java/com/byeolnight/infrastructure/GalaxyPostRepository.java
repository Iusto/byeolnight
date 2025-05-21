package com.byeolnight.infrastructure;

import com.byeolnight.domain.GalaxyPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalaxyPostRepository extends JpaRepository<GalaxyPost, Long> {
}
