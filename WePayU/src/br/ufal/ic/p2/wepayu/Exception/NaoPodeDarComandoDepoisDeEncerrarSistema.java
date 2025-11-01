package br.ufal.ic.p2.wepayu.Exception;

public class NaoPodeDarComandoDepoisDeEncerrarSistema extends Exception {
    public NaoPodeDarComandoDepoisDeEncerrarSistema() {
        super("Nao pode dar comandos depois de encerrarSistema.");
    }

}
