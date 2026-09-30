package com.example.clientes.repository;

import java.util.List;
import java.util.Optional;

import com.example.clientes.model.Cliente;

/** Porta de persistencia usada pelo servico; a implementacao JPA fica em {@code persistence}. */
public interface ClienteRepository {

	Cliente salvar(Cliente cliente);

	boolean existePorCpf(String cpf);

	Optional<Cliente> buscarPorCpf(String cpf);

	List<Cliente> listarTodos();

	List<Cliente> buscarPorNome(String nome);

}
