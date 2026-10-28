package com.acms.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Parent of every repository: all our entities have a Long id.
 * @NoRepositoryBean = Spring must not try to create a repository for this interface itself.
 */
@NoRepositoryBean
public interface BaseRepository<T> extends JpaRepository<T, Long> {
}
