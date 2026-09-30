package com.example.clientes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.clientes.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, String> {

	List<Cliente> findByNomeContainingIgnoreCase(String nome);

}
