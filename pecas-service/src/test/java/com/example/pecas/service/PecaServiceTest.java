package com.example.pecas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.pecas.model.Peca;
import com.example.pecas.repository.PecaRepository;

/** Teste unitario do servico: repositorio mockado, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class PecaServiceTest {

	@Mock
	private PecaRepository repository;

	private SimpleMeterRegistry meterRegistry;

	private PecaService service;

	@BeforeEach
	void setUp() {
		// Registry em memoria (real, sem framework) para verificar as metricas;
		// por isso o servico e criado aqui em vez de usar @InjectMocks.
		meterRegistry = new SimpleMeterRegistry();
		service = new PecaService(repository, meterRegistry);
	}

	private double cadastros(String resultado) {
		return meterRegistry.get("pecas.cadastro").tag("resultado", resultado).counter().count();
	}

	@Test
	@DisplayName("Deve salvar a peça e contar um cadastro com sucesso quando o id é novo")
	void cadastrar_idNovo_salvaEContaSucesso() {
		// Arrange
		Peca peca = new Peca(1L, "Parafuso", "M8");
		when(repository.existePorId(1L)).thenReturn(false);
		when(repository.salvar(peca)).thenReturn(peca);

		// Act
		Peca salva = service.cadastrar(peca);

		// Assert
		assertSame(peca, salva);
		verify(repository).salvar(peca);
		assertEquals(1.0, cadastros("sucesso"));
		assertEquals(0.0, cadastros("duplicado"));
	}

	@Test
	@DisplayName("Deve rejeitar a peça, sem salvar, e contar um duplicado quando o id já existe")
	void cadastrar_idExistente_lancaExcecaoEContaDuplicado() {
		// Arrange
		Peca peca = new Peca(1L, "Parafuso", "M8");
		when(repository.existePorId(1L)).thenReturn(true);

		// Act
		PecaJaCadastradaException excecao = assertThrows(PecaJaCadastradaException.class,
				() -> service.cadastrar(peca));

		// Assert
		assertTrue(excecao.getMessage().contains("1"));
		verify(repository, never()).salvar(any());
		assertEquals(0.0, cadastros("sucesso"));
		assertEquals(1.0, cadastros("duplicado"));
	}

	@Test
	@DisplayName("Deve retornar a peça quando o id existe")
	void buscarPorId_idExistente_retornaPeca() {
		// Arrange
		Peca peca = new Peca(1L, "Parafuso", "M8");
		when(repository.buscarPorId(1L)).thenReturn(Optional.of(peca));

		// Act
		Optional<Peca> encontrada = service.buscarPorId(1L);

		// Assert
		assertTrue(encontrada.isPresent());
		assertSame(peca, encontrada.get());
	}

	@Test
	@DisplayName("Deve retornar vazio quando o id não existe")
	void buscarPorId_idInexistente_retornaVazio() {
		// Arrange
		when(repository.buscarPorId(99L)).thenReturn(Optional.empty());

		// Act
		Optional<Peca> encontrada = service.buscarPorId(99L);

		// Assert
		assertTrue(encontrada.isEmpty());
	}

	@Test
	@DisplayName("Deve listar todas as peças do repositório")
	void listarTodas_retornaPecasDoRepositorio() {
		// Arrange
		List<Peca> pecas = List.of(new Peca(1L, "Parafuso", "M8"), new Peca(2L, "Porca", "M8"));
		when(repository.listarTodas()).thenReturn(pecas);

		// Act
		List<Peca> resultado = service.listarTodas();

		// Assert
		assertEquals(pecas, resultado);
	}

	@Test
	@DisplayName("Deve delegar a busca por nome ao repositório")
	void buscarPorNome_delegaAoRepositorio() {
		// Arrange
		List<Peca> pecas = List.of(new Peca(1L, "Parafuso", "M8"));
		when(repository.buscarPorNome("paraf")).thenReturn(pecas);

		// Act
		List<Peca> resultado = service.buscarPorNome("paraf");

		// Assert
		assertEquals(pecas, resultado);
		verify(repository).buscarPorNome("paraf");
	}

}
