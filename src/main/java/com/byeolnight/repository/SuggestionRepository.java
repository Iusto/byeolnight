package com.byeolnight.repository;

import com.byeolnight.entity.Suggestion;
import com.byeolnight.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SuggestionRepository extends JpaRepository<Suggestion, Long>, JpaSpecificationExecutor<Suggestion> {

    Page<Suggestion> findByAuthor(User author, Pageable pageable);

    long countByAuthor(User author);
}