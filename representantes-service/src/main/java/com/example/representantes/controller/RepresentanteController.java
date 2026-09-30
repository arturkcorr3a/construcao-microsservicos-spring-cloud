package com.example.representantes.controller;

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

import com.example.representantes.model.Representante;
import com.example.representantes.service.RepresentanteJaCadastradoException;
import com.example.representantes.service.RepresentanteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/representantes")
public class RepresentanteController {

	private final RepresentanteService service;

	public RepresentanteController(RepresentanteService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<Representante> cadastrar(@Valid @RequestBody Representante representante) {
		Representante salvo = service.cadastrar(representante);
		return ResponseEntity.created(URI.create("/representantes/" + salvo.getCpf())).body(salvo);
	}

	@GetMapping
	public List<Representante> listar() {
		return service.listarTodos();
	}

	@GetMapping("/{cpf}")
	public Representante consultarPorCpf(@PathVariable String cpf) {
		return service.buscarPorCpf(cpf)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Representante com CPF " + cpf + " nao encontrado"));
	}

	@GetMapping("/busca")
	public List<Representante> consultarPorNome(@RequestParam String nome) {
		return service.buscarPorNome(nome);
	}

	@ExceptionHandler(RepresentanteJaCadastradoException.class)
	public ResponseEntity<ProblemDetail> tratarDuplicado(RepresentanteJaCadastradoException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage()));
	}

}
