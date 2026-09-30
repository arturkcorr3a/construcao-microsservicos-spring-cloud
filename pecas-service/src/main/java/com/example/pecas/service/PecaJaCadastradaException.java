package com.example.pecas.service;

public class PecaJaCadastradaException extends RuntimeException {

	public PecaJaCadastradaException(Long id) {
		super("Ja existe peca com id " + id);
	}

}
