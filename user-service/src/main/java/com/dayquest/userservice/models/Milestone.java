package com.dayquest.userservice.models;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "milestones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "required_invites", nullable = false, unique = true)
    private int requiredInvites;

    @Column(name = "reward_name", nullable = false)
    private String rewardName;

    //TODO: When there are more reward types (like a badge or smth. else) add a field rewardType maybe also implement reward quantity when you want to give more than one item
}