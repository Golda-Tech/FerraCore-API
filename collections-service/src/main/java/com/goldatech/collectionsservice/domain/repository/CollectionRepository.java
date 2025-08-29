package com.goldatech.collectionsservice.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.goldatech.collectionsservice.domain.model.Collection;

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
}
