package com.example.pecas.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Peca do dominio. Nao depende de JPA: o mapeamento para o banco fica em {@code persistence}. */
public class Peca {

	/** Numero de identificacao da peca, informado no cadastro. */
	@NotNull
	private Long id;

	@NotBlank
	private String nome;

	private String descricao;

	public Peca() {
	}

	public Peca(Long id, String nome, String descricao) {
		this.id = id;
		this.nome = nome;
		this.descricao = descricao;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

}
