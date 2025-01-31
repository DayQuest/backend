package com.dayquest.dayquestbackend.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

//Temporary class (only in beta time)
@Component
public class ActivityUpdater {

    @Autowired
    private UserRepository userRepository;

    public void increaseInteractions(User user) {
        user.increaseInteractions();
        userRepository.save(user);
    }

    public void increaseInteractions(Optional<User> optionalUser) {
        optionalUser.ifPresent(this::increaseInteractions);
    }

    public void increaseInteractions(UUID uuid) {
        increaseInteractions(userRepository.findById(uuid));
    }
}
