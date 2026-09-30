package com.example.pecas.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "peca")
public class PecaEntity {

	@Id
	private Long id;

	private String nome;

	private String descricao;

	protected PecaEntity() {
	}

	public PecaEntity(Long id, String nome, String descricao) {
		this.id = id;
		this.nome = nome;
		this.descricao = descricao;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getDescricao() {
		return descricao;
	}

}
