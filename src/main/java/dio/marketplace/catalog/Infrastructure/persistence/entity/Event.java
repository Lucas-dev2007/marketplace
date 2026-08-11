package dio.marketplace.catalog.Infrastructure.persistence.entity;

import dio.marketplace.catalog.Infrastructure.persistence.event.EventListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

import java.util.UUID;

@Entity
@EntityListeners(EventListener.class)
@Data
@RequiredArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String title;


    @Column(nullable = false)
    private Instant date;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdOn;


}
