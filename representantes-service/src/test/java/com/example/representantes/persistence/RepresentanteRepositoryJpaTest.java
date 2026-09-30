package com.example.representantes.persistence;

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

import com.example.representantes.model.Representante;

/**
 * Teste unitario do codigo de persistencia isolando o framework (JPA/Hibernate) e o banco:
 * o repositorio Spring Data e mockado; verifica-se a conversao Representante <-> RepresentanteEntity e a delegacao.
 */
@ExtendWith(MockitoExtension.class)
class RepresentanteRepositoryJpaTest {

	private static final String CPF = "12345678901";

	@Mock
	private RepresentanteJpaRepository jpaRepository;

	@InjectMocks
	private RepresentanteRepositoryJpa repository;

	@Test
	@DisplayName("Salvar deve converter o representante em entidade, salvar e devolver o representante do domínio")
	void salvar_converteParaEntidadeEDevolveDominio() {
		// Arrange
		when(jpaRepository.save(any(RepresentanteEntity.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		ArgumentCaptor<RepresentanteEntity> entidadeSalva = ArgumentCaptor.forClass(RepresentanteEntity.class);

		// Act
		Representante salvo = repository.salvar(new Representante(CPF, "Carlos Lima"));

		// Assert
		verify(jpaRepository).save(entidadeSalva.capture());
		assertEquals(CPF, entidadeSalva.getValue().getCpf());
		assertEquals("Carlos Lima", entidadeSalva.getValue().getNome());
		assertEquals(CPF, salvo.getCpf());
		assertEquals("Carlos Lima", salvo.getNome());
	}

	@Test
	@DisplayName("Buscar por CPF deve converter a entidade encontrada")
	void buscarPorCpf_entidadeExistente_retornaRepresentante() {
		// Arrange
		when(jpaRepository.findById(CPF)).thenReturn(Optional.of(new RepresentanteEntity(CPF, "Carlos Lima")));

		// Act
		Optional<Representante> encontrado = repository.buscarPorCpf(CPF);

		// Assert
		assertTrue(encontrado.isPresent());
		assertEquals("Carlos Lima", encontrado.get().getNome());
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
				new RepresentanteEntity(CPF, "Carlos Lima"), new RepresentanteEntity("98765432100", "Ana Pereira")));

		// Act
		List<Representante> lista = repository.listarTodos();

		// Assert
		assertEquals(List.of(CPF, "98765432100"), lista.stream().map(Representante::getCpf).toList());
	}

	@Test
	@DisplayName("Buscar por nome deve usar a consulta parcial sem diferenciar maiúsculas")
	void buscarPorNome_usaConsultaDerivada() {
		// Arrange
		when(jpaRepository.findByNomeContainingIgnoreCase("lima"))
				.thenReturn(List.of(new RepresentanteEntity(CPF, "Carlos Lima")));

		// Act
		List<Representante> lista = repository.buscarPorNome("lima");

		// Assert
		assertEquals(1, lista.size());
		assertEquals("Carlos Lima", lista.get(0).getNome());
	}

}
