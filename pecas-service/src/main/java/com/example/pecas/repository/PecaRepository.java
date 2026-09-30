package com.example.pecas.repository;

import java.util.List;
import java.util.Optional;

import com.example.pecas.model.Peca;

/** Porta de persistencia usada pelo servico; a implementacao JPA fica em {@code persistence}. */
public interface PecaRepository {

	Peca salvar(Peca peca);

	boolean existePorId(Long id);

	Optional<Peca> buscarPorId(Long id);

	List<Peca> listarTodas();

	List<Peca> buscarPorNome(String nome);

}
