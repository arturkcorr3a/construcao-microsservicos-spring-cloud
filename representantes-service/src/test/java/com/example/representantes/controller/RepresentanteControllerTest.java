package com.example.representantes.controller;

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

import com.example.representantes.model.Representante;
import com.example.representantes.service.RepresentanteJaCadastradoException;
import com.example.representantes.service.RepresentanteService;

/**
 * Teste unitario do controller isolando o framework web: os metodos sao chamados
 * diretamente, sem servlet, MockMvc ou contexto Spring. O servico e mockado.
 */
@ExtendWith(MockitoExtension.class)
class RepresentanteControllerTest {

	private static final String CPF = "12345678901";

	@Mock
	private RepresentanteService service;

	@InjectMocks
	private RepresentanteController controller;

	@Test
	@DisplayName("Cadastrar deve responder 201 com Location e o representante salvo no corpo")
	void cadastrar_representanteValido_retorna201ComLocation() {
		// Arrange
		Representante representante = new Representante(CPF, "Carlos Lima");
		when(service.cadastrar(representante)).thenReturn(representante);

		// Act
		ResponseEntity<Representante> resposta = controller.cadastrar(representante);

		// Assert
		assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
		assertEquals(URI.create("/representantes/" + CPF), resposta.getHeaders().getLocation());
		assertSame(representante, resposta.getBody());
	}

	@Test
	@DisplayName("Cadastrar deve propagar a exceção de duplicado para o handler")
	void cadastrar_representanteDuplicado_propagaExcecao() {
		// Arrange
		Representante representante = new Representante(CPF, "Carlos Lima");
		when(service.cadastrar(representante)).thenThrow(new RepresentanteJaCadastradoException(CPF));

		// Act & Assert
		assertThrows(RepresentanteJaCadastradoException.class, () -> controller.cadastrar(representante));
	}

	@Test
	@DisplayName("O handler de duplicado deve responder 409 com a mensagem no detalhe")
	void tratarDuplicado_retorna409() {
		// Act
		ResponseEntity<ProblemDetail> resposta = controller.tratarDuplicado(new RepresentanteJaCadastradoException(CPF));

		// Assert
		assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
		assertEquals(409, resposta.getBody().getStatus());
		assertTrue(resposta.getBody().getDetail().contains(CPF));
	}

	@Test
	@DisplayName("Consultar por CPF deve retornar o representante quando ele existe")
	void consultarPorCpf_cpfExistente_retornaRepresentante() {
		// Arrange
		Representante representante = new Representante(CPF, "Carlos Lima");
		when(service.buscarPorCpf(CPF)).thenReturn(Optional.of(representante));

		// Act
		Representante resultado = controller.consultarPorCpf(CPF);

		// Assert
		assertSame(representante, resultado);
	}

	@Test
	@DisplayName("Consultar por CPF deve lançar 404 quando o representante não existe")
	void consultarPorCpf_cpfInexistente_lanca404() {
		// Arrange
		when(service.buscarPorCpf("000")).thenReturn(Optional.empty());

		// Act
		ResponseStatusException excecao = assertThrows(ResponseStatusException.class,
				() -> controller.consultarPorCpf("000"));

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, excecao.getStatusCode());
	}

	@Test
	@DisplayName("Listar deve retornar os representantes do serviço")
	void listar_retornaRepresentantesDoServico() {
		// Arrange
		List<Representante> lista = List.of(new Representante(CPF, "Carlos Lima"));
		when(service.listarTodos()).thenReturn(lista);

		// Act
		List<Representante> resultado = controller.listar();

		// Assert
		assertEquals(lista, resultado);
	}

	@Test
	@DisplayName("Consultar por nome deve repassar o termo ao serviço")
	void consultarPorNome_repassaTermoAoServico() {
		// Arrange
		List<Representante> lista = List.of(new Representante(CPF, "Carlos Lima"));
		when(service.buscarPorNome("lima")).thenReturn(lista);

		// Act
		List<Representante> resultado = controller.consultarPorNome("lima");

		// Assert
		assertEquals(lista, resultado);
		verify(service).buscarPorNome("lima");
	}

}
