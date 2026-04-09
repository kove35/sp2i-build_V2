package com.sp2i.infrastructure.persistence;

import com.sp2i.domain.capex.ImportParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImportParameterRepository extends JpaRepository<ImportParameter, Integer> {

    Optional<ImportParameter> findByCode(String code);
}
