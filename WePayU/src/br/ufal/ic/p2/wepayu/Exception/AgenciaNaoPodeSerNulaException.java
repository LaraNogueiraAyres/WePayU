package br.ufal.ic.p2.wepayu.Exception;

public class AgenciaNaoPodeSerNulaException extends Exception {
    public AgenciaNaoPodeSerNulaException() {
        super("Agencia nao pode ser nulo.");
    }

}
