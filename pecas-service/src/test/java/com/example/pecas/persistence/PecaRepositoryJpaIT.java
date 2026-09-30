package com.example.pecas.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.example.pecas.model.Peca;

/** Teste de integracao da persistencia: JPA real contra H2 em memoria, com rollback a cada teste. */
@DataJpaTest
@Import(PecaRepositoryJpa.class)
class PecaRepositoryJpaIT {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private PecaRepositoryJpa repository;

	@Test
	@DisplayName("Salvar deve gravar a peça no banco")
	void salvar_gravaNoBanco() {
		// Act
		repository.salvar(new Peca(1L, "Parafuso", "M8"));
		entityManager.flush();
		entityManager.clear();

		// Assert
		PecaEntity gravada = entityManager.find(PecaEntity.class, 1L);
		assertThat(gravada).isNotNull();
		assertThat(gravada.getNome()).isEqualTo("Parafuso");
		assertThat(gravada.getDescricao()).isEqualTo("M8");
	}

	@Test
	@DisplayName("Buscar por id deve encontrar a peça após persisti-la")
	void buscarPorId_aposPersistir_retornaPeca() {
		// Arrange
		entityManager.persistAndFlush(new PecaEntity(1L, "Parafuso", "M8"));

		// Act
		Optional<Peca> encontrada = repository.buscarPorId(1L);

		// Assert
		assertThat(encontrada).isPresent();
		assertThat(encontrada.get().getNome()).isEqualTo("Parafuso");
	}

	@Test
	@DisplayName("Existe por id deve refletir o que está no banco")
	void existePorId_refleteOBanco() {
		// Arrange
		entityManager.persistAndFlush(new PecaEntity(1L, "Parafuso", "M8"));

		// Act & Assert
		assertThat(repository.existePorId(1L)).isTrue();
		assertThat(repository.existePorId(2L)).isFalse();
	}

	@Test
	@DisplayName("Buscar por nome deve encontrar trechos do nome sem diferenciar maiúsculas")
	void buscarPorNome_trechoSemDiferenciarMaiusculas() {
		// Arrange
		entityManager.persist(new PecaEntity(1L, "Parafuso sextavado", "M8"));
		entityManager.persist(new PecaEntity(2L, "Porca", "M8"));
		entityManager.persist(new PecaEntity(3L, "PARAFUSO de rosca", "M10"));
		entityManager.flush();

		// Act & Assert
		assertThat(repository.buscarPorNome("parafuso")).extracting(Peca::getId).containsExactlyInAnyOrder(1L, 3L);
		assertThat(repository.buscarPorNome("xyz")).isEmpty();
	}

	@Test
	@DisplayName("Listar todas deve retornar todas as peças persistidas")
	void listarTodas_retornaTodas() {
		// Arrange
		entityManager.persist(new PecaEntity(1L, "Parafuso", "M8"));
		entityManager.persist(new PecaEntity(2L, "Porca", "M8"));
		entityManager.flush();

		// Act & Assert
		assertThat(repository.listarTodas()).extracting(Peca::getId).containsExactlyInAnyOrder(1L, 2L);
	}

}
