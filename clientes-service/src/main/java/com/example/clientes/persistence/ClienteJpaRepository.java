package com.example.clientes.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, String> {

	List<ClienteEntity> findByNomeContainingIgnoreCase(String nome);

}
