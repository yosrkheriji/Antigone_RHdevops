package com.antigone.rh.repository;

import com.antigone.rh.entity.ServiceCatalogue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceCatalogueRepository extends JpaRepository<ServiceCatalogue, Long> {
    List<ServiceCatalogue> findAllByActifTrue();
}
