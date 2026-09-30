package com.example.pecas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.pecas.model.Peca;

public interface PecaRepository extends JpaRepository<Peca, Long> {

	List<Peca> findByNomeContainingIgnoreCase(String nome);

}
