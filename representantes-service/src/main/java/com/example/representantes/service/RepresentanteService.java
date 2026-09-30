package com.example.representantes.service;

import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Service;

import com.example.representantes.model.Representante;
import com.example.representantes.repository.RepresentanteRepository;

@Service
public class RepresentanteService {

	private final RepresentanteRepository repository;
	private final Counter cadastrosComSucesso;
	private final Counter cadastrosDuplicados;

	public RepresentanteService(RepresentanteRepository repository, MeterRegistry meterRegistry) {
		this.repository = repository;
		// Exportado no Prometheus como representantes_cadastro_total{resultado="..."}
		this.cadastrosComSucesso = Counter.builder("representantes.cadastro")
				.description("Tentativas de cadastro de representantes, por resultado")
				.tag("resultado", "sucesso")
				.register(meterRegistry);
		this.cadastrosDuplicados = Counter.builder("representantes.cadastro")
				.description("Tentativas de cadastro de representantes, por resultado")
				.tag("resultado", "duplicado")
				.register(meterRegistry);
	}

	public Representante cadastrar(Representante representante) {
		if (repository.existePorCpf(representante.getCpf())) {
			cadastrosDuplicados.increment();
			throw new RepresentanteJaCadastradoException(representante.getCpf());
		}
		Representante salvo = repository.salvar(representante);
		cadastrosComSucesso.increment();
		return salvo;
	}

	public List<Representante> listarTodos() {
		return repository.listarTodos();
	}

	public Optional<Representante> buscarPorCpf(String cpf) {
		return repository.buscarPorCpf(cpf);
	}

	public List<Representante> buscarPorNome(String nome) {
		return repository.buscarPorNome(nome);
	}

}
