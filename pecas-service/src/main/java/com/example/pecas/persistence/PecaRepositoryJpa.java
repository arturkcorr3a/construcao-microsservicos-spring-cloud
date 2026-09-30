package com.example.pecas.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.pecas.model.Peca;
import com.example.pecas.repository.PecaRepository;

/** Implementa a porta {@link PecaRepository} com Spring Data JPA, convertendo Peca <-> PecaEntity. */
@Repository
public class PecaRepositoryJpa implements PecaRepository {

	private final PecaJpaRepository jpaRepository;

	public PecaRepositoryJpa(PecaJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Peca salvar(Peca peca) {
		return paraDominio(jpaRepository.save(paraEntidade(peca)));
	}

	@Override
	public boolean existePorId(Long id) {
		return jpaRepository.existsById(id);
	}

	@Override
	public Optional<Peca> buscarPorId(Long id) {
		return jpaRepository.findById(id).map(PecaRepositoryJpa::paraDominio);
	}

	@Override
	public List<Peca> listarTodas() {
		return jpaRepository.findAll().stream().map(PecaRepositoryJpa::paraDominio).toList();
	}

	@Override
	public List<Peca> buscarPorNome(String nome) {
		return jpaRepository.findByNomeContainingIgnoreCase(nome).stream().map(PecaRepositoryJpa::paraDominio).toList();
	}

	private static PecaEntity paraEntidade(Peca peca) {
		return new PecaEntity(peca.getId(), peca.getNome(), peca.getDescricao());
	}

	private static Peca paraDominio(PecaEntity entidade) {
		return new Peca(entidade.getId(), entidade.getNome(), entidade.getDescricao());
	}

}
