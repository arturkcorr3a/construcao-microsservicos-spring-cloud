package com.example.pecas.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.pecas.model.Peca;
import com.example.pecas.service.PecaJaCadastradaException;
import com.example.pecas.service.PecaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/pecas")
public class PecaController {

	private final PecaService service;

	public PecaController(PecaService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<Peca> cadastrar(@Valid @RequestBody Peca peca) {
		Peca salva = service.cadastrar(peca);
		return ResponseEntity.created(URI.create("/pecas/" + salva.getId())).body(salva);
	}

	@GetMapping
	public List<Peca> listar() {
		return service.listarTodas();
	}

	@GetMapping("/{id}")
	public Peca consultarPorId(@PathVariable Long id) {
		return service.buscarPorId(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Peca " + id + " nao encontrada"));
	}

	@GetMapping("/busca")
	public List<Peca> consultarPorNome(@RequestParam String nome) {
		return service.buscarPorNome(nome);
	}

	@ExceptionHandler(PecaJaCadastradaException.class)
	public ResponseEntity<ProblemDetail> tratarDuplicado(PecaJaCadastradaException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage()));
	}

}
