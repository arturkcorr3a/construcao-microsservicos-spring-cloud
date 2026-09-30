package com.example.representantes.model;

import jakarta.validation.constraints.NotBlank;

/** Representante do dominio. Nao depende de JPA: o mapeamento para o banco fica em {@code persistence}. */
public class Representante {

	@NotBlank
	private String cpf;

	@NotBlank
	private String nome;

	public Representante() {
	}

	public Representante(String cpf, String nome) {
		this.cpf = cpf;
		this.nome = nome;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

}
