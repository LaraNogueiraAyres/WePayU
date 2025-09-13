package br.ufal.ic.p2.wepayu.Exception;

public class SalarioNaoPodeSerNuloException extends Exception {
    public SalarioNaoPodeSerNuloException() {
        super("Salario nao pode ser nulo.");
    }
}

