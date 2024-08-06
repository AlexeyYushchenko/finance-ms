package ru.utlc.financialmanagementservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Here's a quick rundown of the annotations and their functions:
 *
 * @MappedSuperclass: Specifies that this is a mapped superclass and its attributes will be inherited by the child entities.
 *
 * @EntityListeners(AuditingEntityListener.class): Specifies the listener for auditing. It will automatically fill in the @CreatedBy, @LastModifiedBy, @CreatedDate, @LastModifiedDate annotated fields when we use the repository to save/update a domain object.
 *
 * @CreatedDate, @LastModifiedDate: Annotations to mark the fields to be auto-populated with the date of creation and last modification.
 *
 * @CreatedBy, @LastModifiedBy: Annotations to mark the fields to be auto-populated with the "who" created or modified the entity.
 * @param <T>
 */

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditingEntity<T extends Serializable> implements BaseEntity<T> {

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @LastModifiedBy
    @Column(name = "modified_by")
    private String modifiedBy;
}



