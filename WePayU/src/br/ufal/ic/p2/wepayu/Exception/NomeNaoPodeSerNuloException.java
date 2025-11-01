package br.ufal.ic.p2.wepayu.Exception;

public class NomeNaoPodeSerNuloException extends Exception {
    public NomeNaoPodeSerNuloException() {
        super("Nome nao pode ser nulo.");
    }
}
