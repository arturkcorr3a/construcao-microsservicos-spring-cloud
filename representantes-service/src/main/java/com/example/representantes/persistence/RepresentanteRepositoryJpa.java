package com.example.representantes.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.representantes.model.Representante;
import com.example.representantes.repository.RepresentanteRepository;

/** Implementa a porta {@link RepresentanteRepository} com Spring Data JPA, convertendo Representante <-> RepresentanteEntity. */
@Repository
public class RepresentanteRepositoryJpa implements RepresentanteRepository {

	private final RepresentanteJpaRepository jpaRepository;

	public RepresentanteRepositoryJpa(RepresentanteJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Representante salvar(Representante representante) {
		return paraDominio(jpaRepository.save(paraEntidade(representante)));
	}

	@Override
	public boolean existePorCpf(String cpf) {
		return jpaRepository.existsById(cpf);
	}

	@Override
	public Optional<Representante> buscarPorCpf(String cpf) {
		return jpaRepository.findById(cpf).map(RepresentanteRepositoryJpa::paraDominio);
	}

	@Override
	public List<Representante> listarTodos() {
		return jpaRepository.findAll().stream().map(RepresentanteRepositoryJpa::paraDominio).toList();
	}

	@Override
	public List<Representante> buscarPorNome(String nome) {
		return jpaRepository.findByNomeContainingIgnoreCase(nome).stream().map(RepresentanteRepositoryJpa::paraDominio).toList();
	}

	private static RepresentanteEntity paraEntidade(Representante representante) {
		return new RepresentanteEntity(representante.getCpf(), representante.getNome());
	}

	private static Representante paraDominio(RepresentanteEntity entidade) {
		return new Representante(entidade.getCpf(), entidade.getNome());
	}

}
