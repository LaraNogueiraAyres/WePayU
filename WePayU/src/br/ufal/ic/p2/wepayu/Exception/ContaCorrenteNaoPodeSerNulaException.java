package br.ufal.ic.p2.wepayu.Exception;

public class ContaCorrenteNaoPodeSerNulaException extends Exception {
    public ContaCorrenteNaoPodeSerNulaException() {
        super("Conta corrente nao pode ser nulo.");
    }

}
