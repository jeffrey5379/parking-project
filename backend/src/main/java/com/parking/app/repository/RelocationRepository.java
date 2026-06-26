package com.parking.app.repository;

import com.parking.app.model.Relocation;
import com.parking.app.model.RelocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelocationRepository extends JpaRepository<Relocation, String> {
    List<Relocation> findByStatus(RelocationStatus status);
}
