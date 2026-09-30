package com.example.representantes.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.example.representantes.model.Representante;

/** Teste de integracao da persistencia: JPA real contra H2 em memoria, com rollback a cada teste. */
@DataJpaTest
@Import(RepresentanteRepositoryJpa.class)
class RepresentanteRepositoryJpaIT {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private RepresentanteRepositoryJpa repository;

	@Test
	@DisplayName("Salvar deve gravar o representante no banco")
	void salvar_gravaNoBanco() {
		// Act
		repository.salvar(new Representante("11111111111", "Carlos Lima"));
		entityManager.flush();
		entityManager.clear();

		// Assert
		RepresentanteEntity gravado = entityManager.find(RepresentanteEntity.class, "11111111111");
		assertThat(gravado).isNotNull();
		assertThat(gravado.getNome()).isEqualTo("Carlos Lima");
	}

	@Test
	@DisplayName("Buscar por CPF deve encontrar o representante após persisti-lo")
	void buscarPorCpf_aposPersistir_retornaRepresentante() {
		// Arrange
		entityManager.persistAndFlush(new RepresentanteEntity("11111111111", "Carlos Lima"));

		// Act
		Optional<Representante> encontrado = repository.buscarPorCpf("11111111111");

		// Assert
		assertThat(encontrado).isPresent();
		assertThat(encontrado.get().getNome()).isEqualTo("Carlos Lima");
	}

	@Test
	@DisplayName("Existe por CPF deve refletir o que está no banco")
	void existePorCpf_refleteOBanco() {
		// Arrange
		entityManager.persistAndFlush(new RepresentanteEntity("11111111111", "Carlos Lima"));

		// Act & Assert
		assertThat(repository.existePorCpf("11111111111")).isTrue();
		assertThat(repository.existePorCpf("22222222222")).isFalse();
	}

	@Test
	@DisplayName("Buscar por nome deve encontrar trechos do nome sem diferenciar maiúsculas")
	void buscarPorNome_trechoSemDiferenciarMaiusculas() {
		// Arrange
		entityManager.persist(new RepresentanteEntity("11111111111", "Carlos Lima"));
		entityManager.persist(new RepresentanteEntity("22222222222", "Ana Pereira"));
		entityManager.persist(new RepresentanteEntity("33333333333", "Pedro LIMA"));
		entityManager.flush();

		// Act & Assert
		assertThat(repository.buscarPorNome("lima")).extracting(Representante::getCpf)
				.containsExactlyInAnyOrder("11111111111", "33333333333");
		assertThat(repository.buscarPorNome("xyz")).isEmpty();
	}

	@Test
	@DisplayName("Listar todos deve retornar todos os representantes persistidos")
	void listarTodos_retornaTodos() {
		// Arrange
		entityManager.persist(new RepresentanteEntity("11111111111", "Carlos Lima"));
		entityManager.persist(new RepresentanteEntity("22222222222", "Ana Pereira"));
		entityManager.flush();

		// Act & Assert
		assertThat(repository.listarTodos()).extracting(Representante::getCpf)
				.containsExactlyInAnyOrder("11111111111", "22222222222");
	}

}
