package com.example.pecas.controller;

import java.net.URI;
import java.util.List;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.pecas.model.Peca;
import com.example.pecas.repository.PecaRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/pecas")
public class PecaController {

	private final PecaRepository repository;
	private final Counter cadastrosComSucesso;
	private final Counter cadastrosDuplicados;

	public PecaController(PecaRepository repository, MeterRegistry meterRegistry) {
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

	@PostMapping
	public ResponseEntity<Peca> cadastrar(@Valid @RequestBody Peca peca) {
		if (repository.existsById(peca.getId())) {
			cadastrosDuplicados.increment();
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe peca com id " + peca.getId());
		}
		Peca salva = repository.save(peca);
		cadastrosComSucesso.increment();
		return ResponseEntity.created(URI.create("/pecas/" + salva.getId())).body(salva);
	}

	@GetMapping
	public List<Peca> listar() {
		return repository.findAll();
	}

	@GetMapping("/{id}")
	public Peca consultarPorId(@PathVariable Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Peca " + id + " nao encontrada"));
	}

	@GetMapping("/busca")
	public List<Peca> consultarPorNome(@RequestParam String nome) {
		return repository.findByNomeContainingIgnoreCase(nome);
	}

}
