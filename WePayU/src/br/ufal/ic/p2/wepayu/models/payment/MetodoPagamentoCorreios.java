package br.ufal.ic.p2.wepayu.models.payment;

/**
 * Classe que representa o metodo de pagamento via correios.
 */
public class MetodoPagamentoCorreios implements MetodoPagamento {
    @Override
    public MetodoPagamentoCorreios clone() {
        try {
            return (MetodoPagamentoCorreios) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
