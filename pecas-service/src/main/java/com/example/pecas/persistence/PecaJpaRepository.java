package com.example.pecas.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PecaJpaRepository extends JpaRepository<PecaEntity, Long> {

	List<PecaEntity> findByNomeContainingIgnoreCase(String nome);

}
