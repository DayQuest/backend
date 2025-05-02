package com.dayquest.dayquestbackend.user.repositories;

import com.dayquest.dayquestbackend.user.ids.FollowId;
import com.dayquest.dayquestbackend.user.models.Follow;
import com.dayquest.dayquestbackend.user.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    @Query("SELECT f.id FROM Follow f WHERE f.user = :user")
    Page<FollowId> findFollowIdsByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT f.id FROM Follow f WHERE f.followed = :user")
    Page<FollowId> findFollowerIdsByUser(@Param("user") User user, Pageable pageable);

}
