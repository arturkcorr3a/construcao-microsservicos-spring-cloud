package com.example.pecas.service;

import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Service;

import com.example.pecas.model.Peca;
import com.example.pecas.repository.PecaRepository;

@Service
public class PecaService {

	private final PecaRepository repository;
	private final Counter cadastrosComSucesso;
	private final Counter cadastrosDuplicados;

	public PecaService(PecaRepository repository, MeterRegistry meterRegistry) {
		this.repository = repository;
		// Exportado no Prometheus como pecas_cadastro_total{resultado="..."}
		this.cadastrosComSucesso = Counter.builder("pecas.cadastro")
				.description("Tentativas de cadastro de pecas, por resultado")
				.tag("resultado", "sucesso")
				.register(meterRegistry);
		this.cadastrosDuplicados = Counter.builder("pecas.cadastro")
				.description("Tentativas de cadastro de pecas, por resultado")
				.tag("resultado", "duplicado")
				.register(meterRegistry);
	}

	public Peca cadastrar(Peca peca) {
		if (repository.existePorId(peca.getId())) {
			cadastrosDuplicados.increment();
			throw new PecaJaCadastradaException(peca.getId());
		}
		Peca salva = repository.salvar(peca);
		cadastrosComSucesso.increment();
		return salva;
	}

	public List<Peca> listarTodas() {
		return repository.listarTodas();
	}

	public Optional<Peca> buscarPorId(Long id) {
		return repository.buscarPorId(id);
	}

	public List<Peca> buscarPorNome(String nome) {
		return repository.buscarPorNome(nome);
	}

}
