package com.example.pecas.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.example.pecas.model.Peca;

/**
 * Teste unitario do codigo de persistencia isolando o framework (JPA/Hibernate) e o banco:
 * o repositorio Spring Data e mockado; verifica-se a conversao Peca <-> PecaEntity e a delegacao.
 */
@ExtendWith(MockitoExtension.class)
class PecaRepositoryJpaTest {

	@Mock
	private PecaJpaRepository jpaRepository;

	@InjectMocks
	private PecaRepositoryJpa repository;

	@Test
	@DisplayName("Salvar deve converter a peça em entidade, salvar e devolver a peça do domínio")
	void salvar_converteParaEntidadeEDevolveDominio() {
		// Arrange
		when(jpaRepository.save(any(PecaEntity.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		ArgumentCaptor<PecaEntity> entidadeSalva = ArgumentCaptor.forClass(PecaEntity.class);

		// Act
		Peca salva = repository.salvar(new Peca(1L, "Parafuso", "M8"));

		// Assert
		verify(jpaRepository).save(entidadeSalva.capture());
		assertEquals(1L, entidadeSalva.getValue().getId());
		assertEquals("Parafuso", entidadeSalva.getValue().getNome());
		assertEquals("M8", entidadeSalva.getValue().getDescricao());
		assertEquals(1L, salva.getId());
		assertEquals("Parafuso", salva.getNome());
		assertEquals("M8", salva.getDescricao());
	}

	@Test
	@DisplayName("Buscar por id deve converter a entidade encontrada")
	void buscarPorId_entidadeExistente_retornaPeca() {
		// Arrange
		when(jpaRepository.findById(1L)).thenReturn(Optional.of(new PecaEntity(1L, "Parafuso", "M8")));

		// Act
		Optional<Peca> encontrada = repository.buscarPorId(1L);

		// Assert
		assertTrue(encontrada.isPresent());
		assertEquals("Parafuso", encontrada.get().getNome());
		assertEquals("M8", encontrada.get().getDescricao());
	}

	@Test
	@DisplayName("Buscar por id deve retornar vazio quando a entidade não existe")
	void buscarPorId_entidadeInexistente_retornaVazio() {
		// Arrange
		when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

		// Act & Assert
		assertTrue(repository.buscarPorId(99L).isEmpty());
	}

	@Test
	@DisplayName("Existe por id deve delegar ao Spring Data")
	void existePorId_delegaAoSpringData() {
		// Arrange
		when(jpaRepository.existsById(1L)).thenReturn(true);

		// Act & Assert
		assertTrue(repository.existePorId(1L));
		verify(jpaRepository).existsById(1L);
	}

	@Test
	@DisplayName("Existe por id deve retornar falso quando o Spring Data não encontra")
	void existePorId_inexistente_retornaFalso() {
		// Arrange
		when(jpaRepository.existsById(99L)).thenReturn(false);

		// Act & Assert
		assertFalse(repository.existePorId(99L));
	}

	@Test
	@DisplayName("Listar todas deve converter todas as entidades")
	void listarTodas_converteEntidades() {
		// Arrange
		when(jpaRepository.findAll()).thenReturn(List.of(
				new PecaEntity(1L, "Parafuso", "M8"), new PecaEntity(2L, "Porca", "M8")));

		// Act
		List<Peca> pecas = repository.listarTodas();

		// Assert
		assertEquals(List.of(1L, 2L), pecas.stream().map(Peca::getId).toList());
	}

	@Test
	@DisplayName("Buscar por nome deve usar a consulta parcial sem diferenciar maiúsculas")
	void buscarPorNome_usaConsultaDerivada() {
		// Arrange
		when(jpaRepository.findByNomeContainingIgnoreCase("paraf"))
				.thenReturn(List.of(new PecaEntity(1L, "Parafuso", "M8")));

		// Act
		List<Peca> pecas = repository.buscarPorNome("paraf");

		// Assert
		assertEquals(1, pecas.size());
		assertEquals("Parafuso", pecas.get(0).getNome());
	}

}
