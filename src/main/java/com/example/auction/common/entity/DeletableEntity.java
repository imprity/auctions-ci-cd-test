package com.example.auction.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class DeletableEntity extends ModifiableEntity {

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private boolean deleted = false;

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.deleted = true;
    }
}
