package com.example.representantes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.representantes.model.Representante;

public interface RepresentanteRepository extends JpaRepository<Representante, String> {

	List<Representante> findByNomeContainingIgnoreCase(String nome);

}
