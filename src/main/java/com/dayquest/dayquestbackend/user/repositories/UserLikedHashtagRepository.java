package com.dayquest.dayquestbackend.user.repositories;

import com.dayquest.dayquestbackend.user.ids.UserLikedHashtagId;
import com.dayquest.dayquestbackend.user.models.UserLikedHashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLikedHashtagRepository extends JpaRepository<UserLikedHashtag, UserLikedHashtagId> {

}
