package com.example.pecas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.example.pecas.model.Peca;
import com.example.pecas.service.PecaJaCadastradaException;
import com.example.pecas.service.PecaService;

/**
 * Teste unitario do controller isolando o framework web: os metodos sao chamados
 * diretamente, sem servlet, MockMvc ou contexto Spring. O servico e mockado.
 */
@ExtendWith(MockitoExtension.class)
class PecaControllerTest {

	@Mock
	private PecaService service;

	@InjectMocks
	private PecaController controller;

	@Test
	@DisplayName("Cadastrar deve responder 201 com Location e a peça salva no corpo")
	void cadastrar_pecaValida_retorna201ComLocation() {
		// Arrange
		Peca peca = new Peca(1L, "Parafuso", "M8");
		when(service.cadastrar(peca)).thenReturn(peca);

		// Act
		ResponseEntity<Peca> resposta = controller.cadastrar(peca);

		// Assert
		assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
		assertEquals(URI.create("/pecas/1"), resposta.getHeaders().getLocation());
		assertSame(peca, resposta.getBody());
	}

	@Test
	@DisplayName("Cadastrar deve propagar a exceção de duplicado para o handler")
	void cadastrar_pecaDuplicada_propagaExcecao() {
		// Arrange
		Peca peca = new Peca(1L, "Parafuso", "M8");
		when(service.cadastrar(peca)).thenThrow(new PecaJaCadastradaException(1L));

		// Act & Assert
		assertThrows(PecaJaCadastradaException.class, () -> controller.cadastrar(peca));
	}

	@Test
	@DisplayName("O handler de duplicado deve responder 409 com a mensagem no detalhe")
	void tratarDuplicado_retorna409() {
		// Act
		ResponseEntity<ProblemDetail> resposta = controller.tratarDuplicado(new PecaJaCadastradaException(1L));

		// Assert
		assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
		assertEquals(409, resposta.getBody().getStatus());
		assertTrue(resposta.getBody().getDetail().contains("1"));
	}

	@Test
	@DisplayName("Consultar por id deve retornar a peça quando ela existe")
	void consultarPorId_idExistente_retornaPeca() {
		// Arrange
		Peca peca = new Peca(1L, "Parafuso", "M8");
		when(service.buscarPorId(1L)).thenReturn(Optional.of(peca));

		// Act
		Peca resultado = controller.consultarPorId(1L);

		// Assert
		assertSame(peca, resultado);
	}

	@Test
	@DisplayName("Consultar por id deve lançar 404 quando a peça não existe")
	void consultarPorId_idInexistente_lanca404() {
		// Arrange
		when(service.buscarPorId(99L)).thenReturn(Optional.empty());

		// Act
		ResponseStatusException excecao = assertThrows(ResponseStatusException.class,
				() -> controller.consultarPorId(99L));

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, excecao.getStatusCode());
	}

	@Test
	@DisplayName("Listar deve retornar as peças do serviço")
	void listar_retornaPecasDoServico() {
		// Arrange
		List<Peca> pecas = List.of(new Peca(1L, "Parafuso", "M8"));
		when(service.listarTodas()).thenReturn(pecas);

		// Act
		List<Peca> resultado = controller.listar();

		// Assert
		assertEquals(pecas, resultado);
	}

	@Test
	@DisplayName("Consultar por nome deve repassar o termo ao serviço")
	void consultarPorNome_repassaTermoAoServico() {
		// Arrange
		List<Peca> pecas = List.of(new Peca(1L, "Parafuso", "M8"));
		when(service.buscarPorNome("paraf")).thenReturn(pecas);

		// Act
		List<Peca> resultado = controller.consultarPorNome("paraf");

		// Assert
		assertEquals(pecas, resultado);
		verify(service).buscarPorNome("paraf");
	}

}
