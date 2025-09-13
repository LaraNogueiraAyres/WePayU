package br.ufal.ic.p2.wepayu.models.payment;

/**
 * Classe que representa o metodo de pagamento 'Em Maos'.
 */
public class MetodoPagamentoEmMaos implements MetodoPagamento {
    @Override
    public MetodoPagamentoEmMaos clone() {
        try {
            return (MetodoPagamentoEmMaos) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
