package com.example.auction.common.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class ModifiableEntity extends CreatableEntity {

    @LastModifiedDate
    private LocalDateTime modifiedAt;
}
