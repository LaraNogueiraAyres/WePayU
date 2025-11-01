package br.ufal.ic.p2.wepayu.Exception;

public class TipoNaoAplicavelException extends Exception{
    public TipoNaoAplicavelException(){
        super("Tipo nao aplicavel.");
    }

}
//Tipo nao existe. Por favor digitar 'horista' ou 'assalariado'