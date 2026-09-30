package com.example.representantes.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepresentanteJpaRepository extends JpaRepository<RepresentanteEntity, String> {

	List<RepresentanteEntity> findByNomeContainingIgnoreCase(String nome);

}
