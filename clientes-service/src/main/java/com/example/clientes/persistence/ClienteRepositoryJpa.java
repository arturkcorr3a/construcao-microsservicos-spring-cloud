package com.example.clientes.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.clientes.model.Cliente;
import com.example.clientes.repository.ClienteRepository;

/** Implementa a porta {@link ClienteRepository} com Spring Data JPA, convertendo Cliente <-> ClienteEntity. */
@Repository
public class ClienteRepositoryJpa implements ClienteRepository {

	private final ClienteJpaRepository jpaRepository;

	public ClienteRepositoryJpa(ClienteJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Cliente salvar(Cliente cliente) {
		return paraDominio(jpaRepository.save(paraEntidade(cliente)));
	}

	@Override
	public boolean existePorCpf(String cpf) {
		return jpaRepository.existsById(cpf);
	}

	@Override
	public Optional<Cliente> buscarPorCpf(String cpf) {
		return jpaRepository.findById(cpf).map(ClienteRepositoryJpa::paraDominio);
	}

	@Override
	public List<Cliente> listarTodos() {
		return jpaRepository.findAll().stream().map(ClienteRepositoryJpa::paraDominio).toList();
	}

	@Override
	public List<Cliente> buscarPorNome(String nome) {
		return jpaRepository.findByNomeContainingIgnoreCase(nome).stream().map(ClienteRepositoryJpa::paraDominio).toList();
	}

	private static ClienteEntity paraEntidade(Cliente cliente) {
		return new ClienteEntity(cliente.getCpf(), cliente.getNome());
	}

	private static Cliente paraDominio(ClienteEntity entidade) {
		return new Cliente(entidade.getCpf(), entidade.getNome());
	}

}
