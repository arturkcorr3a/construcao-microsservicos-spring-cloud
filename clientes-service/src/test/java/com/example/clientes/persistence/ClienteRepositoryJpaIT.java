package com.example.clientes.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.example.clientes.model.Cliente;

/** Teste de integracao da persistencia: JPA real contra H2 em memoria, com rollback a cada teste. */
@DataJpaTest
@Import(ClienteRepositoryJpa.class)
class ClienteRepositoryJpaIT {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private ClienteRepositoryJpa repository;

	@Test
	@DisplayName("Salvar deve gravar o cliente no banco")
	void salvar_gravaNoBanco() {
		// Act
		repository.salvar(new Cliente("11111111111", "Maria Silva"));
		entityManager.flush();
		entityManager.clear();

		// Assert
		ClienteEntity gravado = entityManager.find(ClienteEntity.class, "11111111111");
		assertThat(gravado).isNotNull();
		assertThat(gravado.getNome()).isEqualTo("Maria Silva");
	}

	@Test
	@DisplayName("Buscar por CPF deve encontrar o cliente após persisti-lo")
	void buscarPorCpf_aposPersistir_retornaCliente() {
		// Arrange
		entityManager.persistAndFlush(new ClienteEntity("11111111111", "Maria Silva"));

		// Act
		Optional<Cliente> encontrado = repository.buscarPorCpf("11111111111");

		// Assert
		assertThat(encontrado).isPresent();
		assertThat(encontrado.get().getNome()).isEqualTo("Maria Silva");
	}

	@Test
	@DisplayName("Existe por CPF deve refletir o que está no banco")
	void existePorCpf_refleteOBanco() {
		// Arrange
		entityManager.persistAndFlush(new ClienteEntity("11111111111", "Maria Silva"));

		// Act & Assert
		assertThat(repository.existePorCpf("11111111111")).isTrue();
		assertThat(repository.existePorCpf("22222222222")).isFalse();
	}

	@Test
	@DisplayName("Buscar por nome deve encontrar trechos do nome sem diferenciar maiúsculas")
	void buscarPorNome_trechoSemDiferenciarMaiusculas() {
		// Arrange
		entityManager.persist(new ClienteEntity("11111111111", "Maria Silva"));
		entityManager.persist(new ClienteEntity("22222222222", "Joao Souza"));
		entityManager.persist(new ClienteEntity("33333333333", "Pedro SILVA"));
		entityManager.flush();

		// Act & Assert
		assertThat(repository.buscarPorNome("silva")).extracting(Cliente::getCpf)
				.containsExactlyInAnyOrder("11111111111", "33333333333");
		assertThat(repository.buscarPorNome("xyz")).isEmpty();
	}

	@Test
	@DisplayName("Listar todos deve retornar todos os clientes persistidos")
	void listarTodos_retornaTodos() {
		// Arrange
		entityManager.persist(new ClienteEntity("11111111111", "Maria Silva"));
		entityManager.persist(new ClienteEntity("22222222222", "Joao Souza"));
		entityManager.flush();

		// Act & Assert
		assertThat(repository.listarTodos()).extracting(Cliente::getCpf)
				.containsExactlyInAnyOrder("11111111111", "22222222222");
	}

}
