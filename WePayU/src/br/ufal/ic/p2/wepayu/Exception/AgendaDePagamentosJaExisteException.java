package br.ufal.ic.p2.wepayu.Exception;

public class AgendaDePagamentosJaExisteException extends Exception {
    public AgendaDePagamentosJaExisteException() {
        super("Agenda de pagamentos ja existe");
    }

}
