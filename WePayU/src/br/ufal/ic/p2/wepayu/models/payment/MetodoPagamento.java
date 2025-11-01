package br.ufal.ic.p2.wepayu.models.payment;

import java.io.Serializable;

/**
 * Interface base para todos os metodos de pagamento.
 */


public interface MetodoPagamento extends Serializable, Cloneable {
    MetodoPagamento clone();
}
