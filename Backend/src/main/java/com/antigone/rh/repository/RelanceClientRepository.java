package com.antigone.rh.repository;

import com.antigone.rh.entity.RelanceClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelanceClientRepository extends JpaRepository<RelanceClient, Long> {
    List<RelanceClient> findByFactureIdOrderByDateRelanceDesc(Long factureId);

    List<RelanceClient> findAllByEnvoyeeFalseOrderByDateRelanceAsc();
}
