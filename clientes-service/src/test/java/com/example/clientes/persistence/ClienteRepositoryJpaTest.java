package com.example.clientes.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.clientes.model.Cliente;

/**
 * Teste unitario do codigo de persistencia isolando o framework (JPA/Hibernate) e o banco:
 * o repositorio Spring Data e mockado; verifica-se a conversao Cliente <-> ClienteEntity e a delegacao.
 */
@ExtendWith(MockitoExtension.class)
class ClienteRepositoryJpaTest {

	private static final String CPF = "12345678901";

	@Mock
	private ClienteJpaRepository jpaRepository;

	@InjectMocks
	private ClienteRepositoryJpa repository;

	@Test
	@DisplayName("Salvar deve converter o cliente em entidade, salvar e devolver o cliente do domínio")
	void salvar_converteParaEntidadeEDevolveDominio() {
		// Arrange
		when(jpaRepository.save(any(ClienteEntity.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		ArgumentCaptor<ClienteEntity> entidadeSalva = ArgumentCaptor.forClass(ClienteEntity.class);

		// Act
		Cliente salvo = repository.salvar(new Cliente(CPF, "Maria Silva"));

		// Assert
		verify(jpaRepository).save(entidadeSalva.capture());
		assertEquals(CPF, entidadeSalva.getValue().getCpf());
		assertEquals("Maria Silva", entidadeSalva.getValue().getNome());
		assertEquals(CPF, salvo.getCpf());
		assertEquals("Maria Silva", salvo.getNome());
	}

	@Test
	@DisplayName("Buscar por CPF deve converter a entidade encontrada")
	void buscarPorCpf_entidadeExistente_retornaCliente() {
		// Arrange
		when(jpaRepository.findById(CPF)).thenReturn(Optional.of(new ClienteEntity(CPF, "Maria Silva")));

		// Act
		Optional<Cliente> encontrado = repository.buscarPorCpf(CPF);

		// Assert
		assertTrue(encontrado.isPresent());
		assertEquals("Maria Silva", encontrado.get().getNome());
	}

	@Test
	@DisplayName("Buscar por CPF deve retornar vazio quando a entidade não existe")
	void buscarPorCpf_entidadeInexistente_retornaVazio() {
		// Arrange
		when(jpaRepository.findById("000")).thenReturn(Optional.empty());

		// Act & Assert
		assertTrue(repository.buscarPorCpf("000").isEmpty());
	}

	@Test
	@DisplayName("Existe por CPF deve delegar ao Spring Data")
	void existePorCpf_delegaAoSpringData() {
		// Arrange
		when(jpaRepository.existsById(CPF)).thenReturn(true);

		// Act & Assert
		assertTrue(repository.existePorCpf(CPF));
		verify(jpaRepository).existsById(CPF);
	}

	@Test
	@DisplayName("Listar todos deve converter todas as entidades")
	void listarTodos_converteEntidades() {
		// Arrange
		when(jpaRepository.findAll()).thenReturn(List.of(
				new ClienteEntity(CPF, "Maria Silva"), new ClienteEntity("98765432100", "Joao Souza")));

		// Act
		List<Cliente> lista = repository.listarTodos();

		// Assert
		assertEquals(List.of(CPF, "98765432100"), lista.stream().map(Cliente::getCpf).toList());
	}

	@Test
	@DisplayName("Buscar por nome deve usar a consulta parcial sem diferenciar maiúsculas")
	void buscarPorNome_usaConsultaDerivada() {
		// Arrange
		when(jpaRepository.findByNomeContainingIgnoreCase("silva"))
				.thenReturn(List.of(new ClienteEntity(CPF, "Maria Silva")));

		// Act
		List<Cliente> lista = repository.buscarPorNome("silva");

		// Assert
		assertEquals(1, lista.size());
		assertEquals("Maria Silva", lista.get(0).getNome());
	}

}
