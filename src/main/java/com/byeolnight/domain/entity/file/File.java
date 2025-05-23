
package com.byeolnight.domain.entity.file;

import jakarta.persistence.*;

@Entity
@Table(name = "files")
public class File {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String storedName;

    @Column(nullable = false)
    private String path;

    private Long size;

    // Getters and Setters omitted for brevity
}
