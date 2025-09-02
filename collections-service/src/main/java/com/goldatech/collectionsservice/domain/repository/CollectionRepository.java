package com.goldatech.collectionsservice.domain.repository;

import com.goldatech.collectionsservice.domain.model.CollectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.goldatech.collectionsservice.domain.model.Collection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {

    /**
     * Finds a collection by its internal reference ID.
     * @param collectionRef The internal reference ID.
     * @return An Optional containing the Collection if found.
     */
    Optional<Collection> findByCollectionRef(String collectionRef);

    /**
     * Finds a collection by its external reference ID provided by the payment gateway.
     * @param externalRef The external reference ID.
     * @return An Optional containing the Collection if found.
     */
    Optional<Collection> findByExternalRef(String externalRef);


    /**
     * Finds a collection by the client-provided request ID.
     * @param clientRequestId The client-provided unique request ID.
     * @return An Optional containing the Collection if found.
     */
    Optional<Collection> findByClientRequestId(String clientRequestId);


    Page<Collection> findByInitiatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Collection> findByStatus(CollectionStatus status, Pageable pageable);

    Page<Collection> findByInitiatedAtBetweenAndStatus(LocalDateTime startDate, LocalDateTime endDate, CollectionStatus status, Pageable pageable);

    Page<Collection> findAll(Pageable pageable);

    long countByStatus(CollectionStatus status);
}
