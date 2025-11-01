package br.ufal.ic.p2.wepayu.Exception;

public class BancoNaoPodeSerNuloException extends Exception {
    public BancoNaoPodeSerNuloException() {
        super("Banco nao pode ser nulo.");
    }

}
