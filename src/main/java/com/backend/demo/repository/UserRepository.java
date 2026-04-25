package com.backend.demo.repository;

import com.backend.demo.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    User findByEmail(String email);

    @EntityGraph(attributePaths = {"wishlist", "wishlist.category", "roles"})
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findWithWishlistByEmail(@Param("email") String email);

    long countByIsActive(Boolean isActive);

    Page<User> findByTier(com.backend.demo.model.UserTier tier, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<User> searchByNameOrEmail(@Param("search") String search, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE u.tier = :tier
              AND (
                    LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<User> searchByTierAndNameOrEmail(@Param("tier") com.backend.demo.model.UserTier tier,
                                          @Param("search") String search,
                                          Pageable pageable);

}
