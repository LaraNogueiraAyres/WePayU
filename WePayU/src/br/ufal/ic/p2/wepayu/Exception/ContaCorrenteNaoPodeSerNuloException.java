package br.ufal.ic.p2.wepayu.Exception;

public class ContaCorrenteNaoPodeSerNuloException extends Exception {
    public ContaCorrenteNaoPodeSerNuloException() {
        super("Conta corrente nao pode ser nulo.");
    }

}
