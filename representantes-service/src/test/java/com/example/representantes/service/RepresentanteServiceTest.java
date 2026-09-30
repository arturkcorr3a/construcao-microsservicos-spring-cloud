package com.example.representantes.service;

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

import com.example.representantes.model.Representante;
import com.example.representantes.repository.RepresentanteRepository;

/** Teste unitario do servico: repositorio mockado, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class RepresentanteServiceTest {

	private static final String CPF = "12345678901";

	@Mock
	private RepresentanteRepository repository;

	private SimpleMeterRegistry meterRegistry;

	private RepresentanteService service;

	@BeforeEach
	void setUp() {
		// Registry em memoria (real, sem framework) para verificar as metricas;
		// por isso o servico e criado aqui em vez de usar @InjectMocks.
		meterRegistry = new SimpleMeterRegistry();
		service = new RepresentanteService(repository, meterRegistry);
	}

	private double cadastros(String resultado) {
		return meterRegistry.get("representantes.cadastro").tag("resultado", resultado).counter().count();
	}

	@Test
	@DisplayName("Deve salvar o representante e contar um cadastro com sucesso quando o CPF é novo")
	void cadastrar_cpfNovo_salvaEContaSucesso() {
		// Arrange
		Representante representante = new Representante(CPF, "Carlos Lima");
		when(repository.existePorCpf(CPF)).thenReturn(false);
		when(repository.salvar(representante)).thenReturn(representante);

		// Act
		Representante salvo = service.cadastrar(representante);

		// Assert
		assertSame(representante, salvo);
		verify(repository).salvar(representante);
		assertEquals(1.0, cadastros("sucesso"));
		assertEquals(0.0, cadastros("duplicado"));
	}

	@Test
	@DisplayName("Deve rejeitar o representante, sem salvar, e contar um duplicado quando o CPF já existe")
	void cadastrar_cpfExistente_lancaExcecaoEContaDuplicado() {
		// Arrange
		Representante representante = new Representante(CPF, "Carlos Lima");
		when(repository.existePorCpf(CPF)).thenReturn(true);

		// Act
		RepresentanteJaCadastradoException excecao = assertThrows(RepresentanteJaCadastradoException.class,
				() -> service.cadastrar(representante));

		// Assert
		assertTrue(excecao.getMessage().contains(CPF));
		verify(repository, never()).salvar(any());
		assertEquals(0.0, cadastros("sucesso"));
		assertEquals(1.0, cadastros("duplicado"));
	}

	@Test
	@DisplayName("Deve retornar o representante quando o CPF existe")
	void buscarPorCpf_cpfExistente_retornaRepresentante() {
		// Arrange
		Representante representante = new Representante(CPF, "Carlos Lima");
		when(repository.buscarPorCpf(CPF)).thenReturn(Optional.of(representante));

		// Act
		Optional<Representante> encontrado = service.buscarPorCpf(CPF);

		// Assert
		assertTrue(encontrado.isPresent());
		assertSame(representante, encontrado.get());
	}

	@Test
	@DisplayName("Deve retornar vazio quando o CPF não existe")
	void buscarPorCpf_cpfInexistente_retornaVazio() {
		// Arrange
		when(repository.buscarPorCpf("000")).thenReturn(Optional.empty());

		// Act
		Optional<Representante> encontrado = service.buscarPorCpf("000");

		// Assert
		assertTrue(encontrado.isEmpty());
	}

	@Test
	@DisplayName("Deve listar todos os representantes do repositório")
	void listarTodos_retornaRepresentantesDoRepositorio() {
		// Arrange
		List<Representante> lista = List.of(new Representante(CPF, "Carlos Lima"), new Representante("98765432100", "Ana Pereira"));
		when(repository.listarTodos()).thenReturn(lista);

		// Act
		List<Representante> resultado = service.listarTodos();

		// Assert
		assertEquals(lista, resultado);
	}

	@Test
	@DisplayName("Deve delegar a busca por nome ao repositório")
	void buscarPorNome_delegaAoRepositorio() {
		// Arrange
		List<Representante> lista = List.of(new Representante(CPF, "Carlos Lima"));
		when(repository.buscarPorNome("lima")).thenReturn(lista);

		// Act
		List<Representante> resultado = service.buscarPorNome("lima");

		// Assert
		assertEquals(lista, resultado);
		verify(repository).buscarPorNome("lima");
	}

}
