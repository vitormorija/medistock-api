package br.com.medistock.api.exception;

public class FalhaDePersistenciaException extends RuntimeException {
    public FalhaDePersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
