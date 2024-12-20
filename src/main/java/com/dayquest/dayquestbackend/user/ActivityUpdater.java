package com.dayquest.dayquestbackend.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

//Temporary class (only in beta time)
@Component
public class ActivityUpdater {

    @Autowired
    private UserRepository userRepository;

    public void increaseInteraction(User user) {
        user.increaseInteractions();
        userRepository.save(user);
    }

    public void increaseInteractions(Optional<User> optionalUser) {
        optionalUser.ifPresent(this::increaseInteraction);
    }
}
