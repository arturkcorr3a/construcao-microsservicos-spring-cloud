package com.example.representantes.repository;

import java.util.List;
import java.util.Optional;

import com.example.representantes.model.Representante;

/** Porta de persistencia usada pelo servico; a implementacao JPA fica em {@code persistence}. */
public interface RepresentanteRepository {

	Representante salvar(Representante representante);

	boolean existePorCpf(String cpf);

	Optional<Representante> buscarPorCpf(String cpf);

	List<Representante> listarTodos();

	List<Representante> buscarPorNome(String nome);

}
