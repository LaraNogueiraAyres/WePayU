package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.Exception.*;
import br.ufal.ic.p2.wepayu.models.CartaoDePonto;
import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.EmpregadoAssalariado;
import br.ufal.ic.p2.wepayu.models.EmpregadoComissionado;
import br.ufal.ic.p2.wepayu.models.EmpregadoHorista;
import br.ufal.ic.p2.wepayu.models.MembroSindicato;
import br.ufal.ic.p2.wepayu.models.ResultadoDeVenda;
import br.ufal.ic.p2.wepayu.models.TaxaServico;
import br.ufal.ic.p2.wepayu.models.payment.MetodoPagamento;
import br.ufal.ic.p2.wepayu.models.payment.MetodoPagamentoBanco;
import br.ufal.ic.p2.wepayu.models.payment.MetodoPagamentoCorreios;
import br.ufal.ic.p2.wepayu.models.payment.MetodoPagamentoEmMaos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;


public class SistemaFolha implements Serializable, Cloneable {

    private Map<String, Empregado> empregados;
    private transient Deque<Map<String, Empregado>> undoStack = new ArrayDeque<>();
    private transient Deque<Map<String, Empregado>> redoStack = new ArrayDeque<>();


    public SistemaFolha() {
        this.empregados = new HashMap<>();
    }

    private static final LocalDate ANCHOR = LocalDate.of(2005, 1, 14);
    private transient boolean encerrado = false;

    private final java.util.Set<String> agendasDisponiveis =
            new java.util.LinkedHashSet<>(java.util.Arrays.asList("semanal 5","semanal 2 5","mensal $"));

    private static final java.util.regex.Pattern P_SEMANAL_1 =
            java.util.regex.Pattern.compile("^semanal\\s+([1-7])$");
    private static final java.util.regex.Pattern P_SEMANAL_2 =
            java.util.regex.Pattern.compile("^semanal\\s+([1-9]|[1-4]\\d|5[0-2])\\s+([1-7])$");
    private static final java.util.regex.Pattern P_MENSAL_D =
            java.util.regex.Pattern.compile("^mensal\\s+([1-9]|1\\d|2[0-8])$");


    public void criarAgendaDePagamentos(String descricao) throws Exception {
        if (descricao == null) throw new DescricaoDeAgendaInvalidaException();
        String desc = descricao.trim();

        if (agendasDisponiveis.contains(desc)) {
            throw new AgendaDePagamentosJaExisteException();
        }

        boolean ok =
            "mensal $".equals(desc) ||                      
            P_MENSAL_D.matcher(desc).matches() ||
            P_SEMANAL_1.matcher(desc).matches() ||
            P_SEMANAL_2.matcher(desc).matches();

        if (!ok) throw new DescricaoDeAgendaInvalidaException();

        agendasDisponiveis.add(desc);
    }

    private void ensureNaoEncerrado() throws Exception {
        if (encerrado) {
            throw new NaoPodeDarComandoDepoisDeEncerrarSistema();
        }
    }

    public void encerrarSistema() {
        this.encerrado = true;
    }

    public String getNumeroDeEmpregados() {
    return Integer.toString(this.empregados == null ? 0 : this.empregados.size());
}

    public void zerarSistema() {
        try {
            Map<String, Empregado> __before = deepCopyEmpregados();
            this.empregados.clear();

            this.agendasDisponiveis.clear();
            this.agendasDisponiveis.addAll(
                java.util.Arrays.asList("semanal 5", "semanal 2 5", "mensal $")
            );

            commit(__before);
        } catch (Exception e) {
            this.empregados.clear();
            this.agendasDisponiveis.clear();
            this.agendasDisponiveis.addAll(
                java.util.Arrays.asList("semanal 5", "semanal 2 5", "mensal $")
            );
        }
    }

    public void addEmpregado(String id, String nome, String endereco, String tipo, String salario, String comissao) throws Exception {
        Map<String, Empregado> __before = deepCopyEmpregados();
        if (nome == null || nome.isEmpty()) {
            throw new NomeNaoPodeSerNuloException();
        }

        if (endereco == null || endereco.isEmpty()) {
            throw new EnderecoNaoPodeSerNuloException();
        }

        if (salario == null || salario.trim().isEmpty()) {
            throw new SalarioNaoPodeSerNuloException();
        }

        double salarioValue;
        try {
            salarioValue = Double.parseDouble(salario.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new SalarioDeveSerNumericoException();
        }

        if (salarioValue < 0) {
            throw new SalarioDeveSerNaoNegativoException();
        }

        if (tipo == null || tipo.isEmpty() || (!tipo.equals("horista") && !tipo.equals("assalariado") && !tipo.equals("comissionado"))) {
            throw new TipoInvalidoException();
        }

        if (tipo.equals("comissionado")) {
            if (comissao == null) {
                throw new TipoNaoAplicavelException();
            }
            if (comissao.trim().isEmpty()) {
                throw new ComissaoNaoPodeSerNulaException();
            }
            try {
                double comissaoValue = Double.parseDouble(comissao.replace(',', '.'));
                if (comissaoValue < 0) {
                    throw new ComissaoDeveSerNaoNegativaException();
                }
            } catch (NumberFormatException e) {
                throw new ComissaoDeveSerNumericaException();
            }
        }

        if ((tipo.equals("horista") || tipo.equals("assalariado")) && comissao != null) {
            throw new TipoNaoAplicavelException();
        }

        Empregado novoEmpregado = null;
        switch (tipo) {
            case "horista":
                novoEmpregado = new EmpregadoHorista(nome, endereco, salarioValue);
                break;
            case "assalariado":
                novoEmpregado = new EmpregadoAssalariado(nome, endereco, salarioValue);
                break;
            case "comissionado":
                novoEmpregado = new EmpregadoComissionado(nome, endereco, salarioValue, Double.parseDouble(comissao.replace(',', '.')));
                break;
        }

        this.empregados.put(id, novoEmpregado);
        commit(__before);
    }

    public String getAtributoEmpregado(String empId, String atributo) throws Exception {

        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }

        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }

        Empregado empregado = this.empregados.get(empId);
        switch (atributo) {
            case "nome":
                return empregado.getNome();
            case "endereco":
                return empregado.getEndereco();
            case "tipo":
                if (empregado instanceof EmpregadoHorista) {
                    return "horista";
                } else if (empregado instanceof EmpregadoAssalariado && !(empregado instanceof EmpregadoComissionado)) {
                    return "assalariado";
                } else if (empregado instanceof EmpregadoComissionado) {
                    return "comissionado";
                }
            case "salario":
                return String.format("%.2f", empregado.getSalario()).replace('.', ',');

            case "agendaPagamento": return empregado.getAgendaPagamento();
            
            case "comissao":
                if (empregado instanceof EmpregadoComissionado) {
                    EmpregadoComissionado comissionado = (EmpregadoComissionado) empregado;
                    return String.format("%.2f", comissionado.getTaxaDeComissao()).replace('.', ',');
                } else {
                    throw new EmpregadoNaoEhComissionadoException();
                }
            case "sindicalizado":
                return String.valueOf(empregado.isSindicalizado());
            
            case "metodoPagamento":
                if (empregado.getMetodoPagamentoObjeto() instanceof MetodoPagamentoEmMaos) {
                    return "emMaos";
                } else if (empregado.getMetodoPagamentoObjeto() instanceof MetodoPagamentoBanco) {
                    return "banco";
                } else if (empregado.getMetodoPagamentoObjeto() instanceof MetodoPagamentoCorreios) {
                    return "correios";
                }
            case "banco":
                if (empregado.getMetodoPagamentoObjeto() instanceof MetodoPagamentoBanco) {
                    MetodoPagamentoBanco banco = (MetodoPagamentoBanco) empregado.getMetodoPagamentoObjeto();
                    return banco.getBanco();
                } else {
                    throw new EmpregadoNaoRecebeEmBancoException();
                }
            case "agencia":
                if (empregado.getMetodoPagamentoObjeto() instanceof MetodoPagamentoBanco) {
                    MetodoPagamentoBanco banco = (MetodoPagamentoBanco) empregado.getMetodoPagamentoObjeto();
                    return banco.getAgencia();
                } else {
                    throw new EmpregadoNaoRecebeEmBancoException();
                }
            case "contaCorrente":
                if (empregado.getMetodoPagamentoObjeto() instanceof MetodoPagamentoBanco) {
                    MetodoPagamentoBanco banco = (MetodoPagamentoBanco) empregado.getMetodoPagamentoObjeto();
                    return banco.getContaCorrente();
                } else {
                    throw new EmpregadoNaoRecebeEmBancoException();
                }
            case "idSindicato":
                if(empregado.isSindicalizado()) {
                    return empregado.getMembroSindicato().getId();
                } else {
                    throw new EmpregadoNaoEhSindicalizadoException();
                }
            case "taxaSindical":
                if(empregado.isSindicalizado()) {
                    return String.format("%.2f", empregado.getMembroSindicato().getTaxaSindical()).replace('.', ',');
                } else {
                    throw new EmpregadoNaoEhSindicalizadoException();
                }
            default:
                throw new AtributoNaoExisteException();
    }
    }

    public void removerEmpregado(String empId) throws Exception {
        Map<String, Empregado> __before = deepCopyEmpregados();
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }   
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        this.empregados.remove(empId);
        commit(__before);
    }

    public void lancaCartao(String empId, String data, String horas) throws Exception {
        Map<String, Empregado> __before = deepCopyEmpregados();
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        Empregado empregado = this.empregados.get(empId);
        if (!(empregado instanceof EmpregadoHorista)) {
            throw new EmpregadoNaoEhHoristaException();
        }

        LocalDate dataCartao;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
            dataCartao = LocalDate.parse(data, formatter);
        } catch (DateTimeParseException e) {
            throw new DataInvalidaException();
        }

        if (horas == null || horas.trim().isEmpty()) {
            throw new HorasDevemSerPositivasException();
        }
        double horasTrabalhadas;
        try {
            horasTrabalhadas = Double.parseDouble(horas.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new HorasDevemSerPositivasException();
        }
        if (horasTrabalhadas <= 0) {
            throw new HorasDevemSerPositivasException();
        }

        EmpregadoHorista horista = (EmpregadoHorista) empregado;
        horista.adicionarCartaoDePonto(new CartaoDePonto(dataCartao, horasTrabalhadas));
        commit(__before);
    }

    public void lancaVenda(String empId, String data, String valor) throws Exception {
        Map<String, Empregado> __before = deepCopyEmpregados();
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        Empregado empregado = this.empregados.get(empId);
        if (!(empregado instanceof EmpregadoComissionado)) {
            throw new EmpregadoNaoEhComissionadoException();
        }

        LocalDate dataVenda;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
            dataVenda = LocalDate.parse(data, formatter);
        } catch (DateTimeParseException e) {
            throw new DataInvalidaException();
        }

        if (valor == null || valor.trim().isEmpty()) {
            throw new ValorDeveSerPositivoException();
        }
        double valorVenda;
        try {
            valorVenda = Double.parseDouble(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValorDeveSerPositivoException();
        }
        if (valorVenda <= 0) {
            throw new ValorDeveSerPositivoException();
        }

        EmpregadoComissionado comissionado = (EmpregadoComissionado) empregado;
        comissionado.adicionarResultadoDeVenda(new ResultadoDeVenda(dataVenda, valorVenda));
        commit(__before);
    }

    public void lancaTaxaServico(String sindId, String data, String valor) throws Exception {
        Map<String, Empregado> __before = deepCopyEmpregados();
        if (sindId == null || sindId.isEmpty()) {
            throw new IdentificacaoMembroNulaException();
        }

        Empregado membroSindicato = null;
        for (Empregado emp : this.empregados.values()) {
            if (emp.isSindicalizado() && emp.getMembroSindicato().getId().equals(sindId)) {
                membroSindicato = emp;
                break;
            }
        }

        if (membroSindicato == null) {
            throw new MembroSindicatoNaoExisteException();
        }

        LocalDate dataTaxa;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
            dataTaxa = LocalDate.parse(data, formatter);
        } catch (DateTimeParseException e) {
            throw new DataInvalidaException();
        }

        if (valor == null || valor.trim().isEmpty()) {
            throw new ValorDeveSerPositivoException();
        }
        double valorTaxa;
        try {
            valorTaxa = Double.parseDouble(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValorDeveSerPositivoException();
        }
        if (valorTaxa <= 0) {
            throw new ValorDeveSerPositivoException();
        }

        membroSindicato.getMembroSindicato().adicionarTaxaDeServico(new TaxaServico(dataTaxa, valorTaxa));
        commit(__before);
    }

    public void alteraEmpregado(String empId, String atributo, String valor) throws Exception {
        alteraEmpregadoInterno(empId, atributo, new String[]{valor});
    }

    public void alteraEmpregado(String empId, String atributo, String valor1, String banco, String agencia, String contaCorrente) throws Exception {
        String[] args = {valor1, banco, agencia, contaCorrente};
        alteraEmpregadoInterno(empId, atributo, args);
    }

    public void alteraEmpregado(String empId, String atributo, String valor, String arg1) throws Exception {
        String[] args = {valor, arg1};
        alteraEmpregadoInterno(empId, atributo, args);
    }
    
    public void alteraEmpregado(String empId, String atributo, String arg1, String arg2, String arg3) throws Exception {
        if (atributo.equals("sindicalizado")) {
            String[] args = {arg1, arg2, arg3};
            alteraEmpregadoInterno(empId, atributo, args);
        } else if (atributo.equals("tipo")) {
            String[] args = {arg1, arg2, arg3};
            alteraEmpregadoInterno(empId, atributo, args);
        } else {
             throw new AtributoNaoExisteException();
        }
    }
    
    private void alteraEmpregadoInterno(String empId, String atributo, String[] args) throws Exception {
        Map<String, Empregado> __before = deepCopyEmpregados();
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }

        Empregado empregado = this.empregados.get(empId);
        
        switch (atributo) {
            case "nome":
                if (args.length < 1 || args[0] == null || args[0].isEmpty()) {
                    throw new NomeNaoPodeSerNuloException();
                }
                empregado.setNome(args[0]);
                break;
            case "endereco":
                if (args.length < 1 || args[0] == null || args[0].isEmpty()) {
                    throw new EnderecoNaoPodeSerNuloException();
                }
                empregado.setEndereco(args[0]);
                break;

            case "agendaPagamento": {
                String nova = (args.length > 0 && args[0] != null) ? args[0].trim() : "";
                if (nova.isEmpty() || !agendasDisponiveis.contains(nova)) {
                    throw new AgendadePagamentoNaoEstaDisponivelException();
                }
                empregado.setAgendaPagamento(nova);
                break;
            }


            case "tipo":
                if (args[0] == null || args[0].isEmpty() || (!args[0].equals("horista") && !args[0].equals("assalariado") && !args[0].equals("comissionado"))) {
                    throw new TipoInvalidoException();
                }
                String novoTipo = args[0];

                double salario = empregado.getSalario();
                double comissao = 0.0;

                java.util.List<String> extraArgs = new java.util.ArrayList<>();
                if (args != null) {
                    for (int i = 1; i < args.length; i++) {
                        if (args[i] != null && !args[i].trim().isEmpty()) {
                            extraArgs.add(args[i].trim());
                        }
                    }
                }

                if (extraArgs.size() == 1) {
                    String only = extraArgs.get(0);
                    if (novoTipo.equals("comissionado")) {
                        try {
                            comissao = Double.parseDouble(only.replace(',', '.'));
                            if (comissao < 0) throw new ComissaoDeveSerNaoNegativaException();
                        } catch (NumberFormatException e) {
                            throw new ComissaoDeveSerNumericaException();
                        }
                    } else {
                        try {
                            salario = Double.parseDouble(only.replace(',', '.'));
                            if (salario < 0) throw new SalarioDeveSerNaoNegativoException();
                        } catch (NumberFormatException e) {
                            throw new SalarioDeveSerNumericoException();
                        }
                    }
                } else if (extraArgs.size() >= 2) {
                    String sSal = extraArgs.get(0);
                    String sCom = extraArgs.get(1);
                    try {
                        salario = Double.parseDouble(sSal.replace(',', '.'));
                        if (salario < 0) throw new SalarioDeveSerNaoNegativoException();
                    } catch (NumberFormatException e) {
                        throw new SalarioDeveSerNumericoException();
                    }
                    try {
                        comissao = Double.parseDouble(sCom.replace(',', '.'));
                        if (comissao < 0) throw new ComissaoDeveSerNaoNegativaException();
                    } catch (NumberFormatException e) {
                        throw new ComissaoDeveSerNumericaException();
                    }
                }

                String nome = empregado.getNome();
                String endereco = empregado.getEndereco();
                MetodoPagamento metodoPagamento = empregado.getMetodoPagamentoObjeto();
                MembroSindicato membroSindicato = empregado.getMembroSindicato();

                Empregado novoEmpregado = null;
                switch (novoTipo) {
                    case "horista":
                        novoEmpregado = new EmpregadoHorista(nome, endereco, salario);
                        break;
                    case "assalariado":
                        novoEmpregado = new EmpregadoAssalariado(nome, endereco, salario);
                        break;
                    case "comissionado":
                        novoEmpregado = new EmpregadoComissionado(nome, endereco, salario, comissao);
                        break;
                }
                
                if (metodoPagamento != null) {
                    novoEmpregado.setMetodoPagamento(metodoPagamento);
                }
                if (membroSindicato != null) {
                    novoEmpregado.setSindicalizado(true, membroSindicato.getId(), membroSindicato.getTaxaSindical());
                }

                this.empregados.put(empId, novoEmpregado);
                break;
            case "sindicalizado":
                if (args.length < 1) {
                    throw new AtributoNaoExisteException();
                }
                
                boolean sindicalizado;
                if(args[0].equals("true")) {
                    sindicalizado = true;
                } else if (args[0].equals("false")) {
                    sindicalizado = false;
                } else {
                    throw new ValorInvalidoException();
                }
                
                if (sindicalizado) {
                    if (args.length < 3) {
                        throw new AtributoNaoExisteException();
                    }
                    String idSindicato = args[1];
                    String taxaSindicalStr = args[2];
                    
                    if (idSindicato == null || idSindicato.isEmpty()) {
                        throw new IdentificacaoSindicatoNulaException();
                    }

                    for (Map.Entry<String, Empregado> entry : this.empregados.entrySet()) {
                        if (!entry.getKey().equals(empId) && entry.getValue().isSindicalizado() && entry.getValue().getMembroSindicato().getId().equals(idSindicato)) {
                            throw new HaOutroEmpregadoComEstaIdentificacaoDeSindicatoException();
                        }
                    }

                    double taxaSindical;
                    if (taxaSindicalStr == null || taxaSindicalStr.isEmpty()) {
                        throw new TaxaSindicalNaoPodeSerNulaException();
                    }
                    try {
                        taxaSindical = Double.parseDouble(taxaSindicalStr.replace(',', '.'));
                        if (taxaSindical < 0) {
                            throw new TaxaSindicalDeveSerNaoNegativaException();
                        }
                    } catch (NumberFormatException e) {
                        throw new TaxaSindicalDeveSerNumericaException();
                    }
                    empregado.setSindicalizado(true, idSindicato, taxaSindical);
                } else {
                    empregado.setSindicalizado(false, null, 0.0);
                }
                break;
            case "metodoPagamento":
                String metodo = args[0];
                switch (metodo) {
                    case "emMaos":
                        empregado.setMetodoPagamento(new MetodoPagamentoEmMaos());
                        break;
                    case "banco":
                        String banco = args[1];
                        String agencia = args[2];
                        String conta = args[3];
                        if (banco == null || banco.isEmpty()) throw new BancoNaoPodeSerNuloException();
                        if (agencia == null || agencia.isEmpty()) throw new AgenciaNaoPodeSerNulaException();
                        if (conta == null || conta.isEmpty()) throw new ContaCorrenteNaoPodeSerNulaException();
                        empregado.setMetodoPagamento(new MetodoPagamentoBanco(banco, agencia, conta));
                        break;
                    case "correios":
                        empregado.setMetodoPagamento(new MetodoPagamentoCorreios());
                        break;
                    default:
                        throw new MetodoPagamentoInvalidoException();
                }
                break;
            case "salario":
                 if (args[0] == null || args[0].trim().isEmpty()) {
                    throw new SalarioNaoPodeSerNuloException(); 
                }
                double salarioValue;
                try {
                    salarioValue = Double.parseDouble(args[0].replace(',', '.'));
                } catch (NumberFormatException e) {
                    throw new SalarioDeveSerNumericoException();
                }

                if (salarioValue < 0) {
                    throw new SalarioDeveSerNaoNegativoException();
                }
                empregado.setSalario(salarioValue);
                break;
            case "comissao":
                if (!(empregado instanceof EmpregadoComissionado)) {
                    throw new EmpregadoNaoEhComissionadoException();
                }
                if (args[0] == null || args[0].trim().isEmpty()) {
                    throw new ComissaoNaoPodeSerNulaException();
                }
                double comissaoValue;
                try {
                    comissaoValue = Double.parseDouble(args[0].replace(',', '.'));
                    if (comissaoValue < 0) {
                        throw new ComissaoDeveSerNaoNegativaException();
                    }
                } catch (NumberFormatException e) {
                    throw new ComissaoDeveSerNumericaException();
                }
                ((EmpregadoComissionado) empregado).setTaxaDeComissao(comissaoValue);
                break;
            default:
                throw new AtributoNaoExisteException();
        }
        commit(__before);
    }

    private void readObject(ObjectInputStream in) throws Exception {
    in.defaultReadObject();
    this.undoStack = new ArrayDeque<>();
    this.redoStack = new ArrayDeque<>();
}

    @SuppressWarnings("unchecked")
    private Map<String, Empregado> deepCopyEmpregados() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(this.empregados);
    }
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
        return (Map<String, Empregado>) ois.readObject();
    }
}

    private void commit(Map<String, Empregado> before) {
    undoStack.push(before);
    redoStack.clear();
}

    public void undo() throws Exception {
        ensureNaoEncerrado();
        if (undoStack == null) undoStack = new ArrayDeque<>();
        if (redoStack == null) redoStack = new ArrayDeque<>();
        if (undoStack.isEmpty()) {
            throw new Exception("Nao ha comando a desfazer.");
        }
        
        Map<String, Empregado> atual = deepCopyEmpregados();
        Map<String, Empregado> anterior = undoStack.pop();
        redoStack.push(atual);
        this.empregados = anterior;
    }

    public void redo() throws Exception {
        ensureNaoEncerrado();
        if (undoStack == null) undoStack = new ArrayDeque<>();
        if (redoStack == null) redoStack = new ArrayDeque<>();
        if (redoStack.isEmpty()) {
            throw new Exception("Nao ha comando a refazer.");
        }
        Map<String, Empregado> atual = deepCopyEmpregados();
        Map<String, Empregado> futuro = redoStack.pop();
        undoStack.push(atual);
        this.empregados = futuro;
    }

    public void rodaFolha(String data) throws Exception {
        
        LocalDate d;
        try {
            d = parseDateFlexible(data);
        } catch (DateTimeParseException e) {
            throw new DataInvalidaException();
        }
        String nome = String.format("folha-%04d-%02d-%02d.txt", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
        rodaFolha(data, nome);
}
    
    private static BigDecimal r2(BigDecimal v) {
        return v.setScale(2, RoundingMode.DOWN);
    }

    public void rodaFolha(String data, String saida) throws Exception {

    Map<String, Empregado> __before = deepCopyEmpregados();
    if (data == null || data.trim().isEmpty()) throw new DataInvalidaException();
    final LocalDate currentDate;
    try { currentDate = parseDateFlexible(data); }
    catch (DateTimeParseException e) { throw new DataInvalidaException(); }

    
    List<Empregado> emps = new ArrayList<>(this.empregados.values());
    emps.sort(Comparator.comparing(Empregado::getNome, String.CASE_INSENSITIVE_ORDER));

    
    StringBuilder horistasLines = new StringBuilder();
    BigDecimal totalHoristasNormalHours = BigDecimal.ZERO;
    BigDecimal totalHoristasExtraHours  = BigDecimal.ZERO;
    BigDecimal totalHoristasBruto       = BigDecimal.ZERO;
    BigDecimal totalHoristasDescontos   = BigDecimal.ZERO;
    BigDecimal totalHoristasLiquido     = BigDecimal.ZERO;

    StringBuilder assalariadosLines = new StringBuilder();
    BigDecimal totalAssalBruto = BigDecimal.ZERO, totalAssalDesc = BigDecimal.ZERO, totalAssalLiq = BigDecimal.ZERO;

    StringBuilder comissionadosLines = new StringBuilder();
    BigDecimal totalComFixo = BigDecimal.ZERO, totalComVend = BigDecimal.ZERO, totalComCom = BigDecimal.ZERO;
    BigDecimal totalComBruto = BigDecimal.ZERO, totalComDesc = BigDecimal.ZERO, totalComLiq = BigDecimal.ZERO;

    
    var sym = new java.text.DecimalFormatSymbols();
    sym.setDecimalSeparator(',');
    var df2 = new java.text.DecimalFormat("0.00"); df2.setDecimalFormatSymbols(sym);
    var df1 = new java.text.DecimalFormat("0.0");  df1.setDecimalFormatSymbols(sym);

    
    final boolean isHoristaPayday      = currentDate.getDayOfWeek() == DayOfWeek.FRIDAY;
    final boolean isAssalariadoPayday  = isLastBusinessDay(currentDate);
    final boolean isComissionadoPayday = !currentDate.isBefore(ANCHOR)
            && currentDate.getDayOfWeek() == DayOfWeek.FRIDAY
            && java.time.temporal.ChronoUnit.WEEKS.between(ANCHOR, currentDate) % 2 == 0;

    for (Empregado emp : emps) {

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;
        double normalHours = 0.0, extraHours = 0.0;
        @SuppressWarnings("unused")
        boolean isPayday = false;

        if (emp instanceof EmpregadoHorista) {
            if (isHoristaPayday) {
                isPayday = true;
                LocalDate periodStart = currentDate.minusDays(6);
                LocalDate periodEndEx = currentDate.plusDays(1);

                EmpregadoHorista h = (EmpregadoHorista) emp;
                BigDecimal hourly = BigDecimal.valueOf(h.getSalario());

                Map<LocalDate, Double> hoursByDay = new HashMap<>();
                for (CartaoDePonto c : h.cartoesDePonto) {
                    LocalDate d2 = c.getData();
                    if (!d2.isBefore(periodStart) && d2.isBefore(periodEndEx)) {
                        hoursByDay.merge(d2, c.getHoras(), Double::sum);
                    }
                }
                for (double hrs : hoursByDay.values()) {
                    normalHours += Math.min(hrs, 8.0);
                    if (hrs > 8.0) extraHours += (hrs - 8.0);
                }

                BigDecimal normalBD = BigDecimal.valueOf(normalHours);
                BigDecimal extraBD  = BigDecimal.valueOf(extraHours);
                gross = normalBD.multiply(hourly)
                        .add(extraBD.multiply(hourly).multiply(BigDecimal.valueOf(1.5)))
                        .setScale(2, RoundingMode.DOWN);

                if (emp.isSindicalizado()) {
                    MembroSindicato ms = emp.getMembroSindicato();
                    if (gross.signum() > 0) {
                        
                        LocalDate from = ms.getUltimaDataCobranca() != null ? ms.getUltimaDataCobranca() : periodStart;
                        long days = java.time.temporal.ChronoUnit.DAYS.between(from, periodEndEx);
                        if (days > 0) {
                            deductions = deductions.add(r2(BigDecimal.valueOf(ms.getTaxaSindical()).multiply(BigDecimal.valueOf(days))));
                            ms.setUltimaDataCobranca(periodEndEx);
                        }
                        
                        for (TaxaServico ts : ms.getTaxasDeServico()) {
                            LocalDate d2 = ts.getData();
                            boolean devidoAteAqui = !d2.isAfter(periodEndEx.minusDays(1));
                            if (devidoAteAqui && !ts.isCobrada()) {
                                deductions = deductions.add(r2(BigDecimal.valueOf(ts.getValor())));
                                ts.marcarCobrada();
                            }
                        }
                    }
                }

                String metodoStr = metodoPagamentoStr(emp.getMetodoPagamentoObjeto(), emp);
                String hNorm = normalHours == Math.floor(normalHours) ? String.valueOf((int) normalHours) : df1.format(normalHours);
                String hExt  = extraHours  == Math.floor(extraHours)  ? String.valueOf((int) extraHours)  : df1.format(extraHours);
                horistasLines.append(String.format(
                        "%-36s %5s %5s %13s %9s %15s %s%n",
                        emp.getNome(),
                        hNorm,
                        hExt,
                        df2.format(gross),
                        df2.format(deductions),
                        df2.format(gross.subtract(deductions).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)),
                        metodoStr
                ));
                totalHoristasNormalHours = totalHoristasNormalHours.add(BigDecimal.valueOf(normalHours));
                totalHoristasExtraHours  = totalHoristasExtraHours.add(BigDecimal.valueOf(extraHours));
                totalHoristasBruto       = totalHoristasBruto.add(gross);
                totalHoristasDescontos   = totalHoristasDescontos.add(deductions);
                totalHoristasLiquido     = totalHoristasLiquido.add(gross.subtract(deductions).max(BigDecimal.ZERO));

            }
        } else if (emp instanceof EmpregadoComissionado) {
            if (isComissionadoPayday) {
                isPayday = true;

                LocalDate periodStart = currentDate.minusDays(13);
                LocalDate periodEndEx = currentDate.plusDays(1);

                EmpregadoComissionado c = (EmpregadoComissionado) emp;
                BigDecimal fixo2Sem = BigDecimal.valueOf(c.getSalario())
                    .multiply(BigDecimal.valueOf(24))
                    .divide(BigDecimal.valueOf(52), 10, RoundingMode.HALF_UP); 
                    fixo2Sem = r2(fixo2Sem);

                BigDecimal vendas = BigDecimal.ZERO;
                for (ResultadoDeVenda v : c.getResultadosDeVenda()) {
                    LocalDate d2 = v.getData();
                    if (!d2.isBefore(periodStart) && d2.isBefore(periodEndEx)) {
                        vendas = vendas.add(BigDecimal.valueOf(v.getValor()));
                    }
                }
                vendas = r2(vendas);

                BigDecimal comissao = r2(vendas.multiply(BigDecimal.valueOf(c.getTaxaDeComissao())));
                gross = r2(fixo2Sem.add(comissao));

                if (emp.isSindicalizado() && gross.signum() > 0) {
                    MembroSindicato ms = emp.getMembroSindicato();
                    long days = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEndEx);
                    deductions = deductions.add(BigDecimal.valueOf(ms.getTaxaSindical()).multiply(BigDecimal.valueOf(days)));
                    for (TaxaServico ts : ms.getTaxasDeServico()) {
                        LocalDate d2 = ts.getData();
                        if (!d2.isBefore(periodStart) && d2.isBefore(periodEndEx)) {
                            deductions = deductions.add(BigDecimal.valueOf(ts.getValor()));
                        }
                    }
                }

                BigDecimal net = r2(gross.subtract(deductions));
                String metodoStr = metodoPagamentoStr(emp.getMetodoPagamentoObjeto(), emp);

                comissionadosLines.append(String.format(
                        "%-21s %8s %8s %8s %13s %9s %15s %s%n",
                        emp.getNome(),
                        df2.format(fixo2Sem),
                        df2.format(vendas),
                        df2.format(comissao),
                        df2.format(gross),
                        df2.format(deductions),
                        df2.format(net),
                        metodoStr
                ));
                totalComFixo  = totalComFixo.add(fixo2Sem);
                totalComVend  = totalComVend.add(vendas);
                totalComCom   = totalComCom.add(comissao);
                totalComBruto = totalComBruto.add(gross);
                totalComDesc  = totalComDesc.add(deductions);
                totalComLiq   = totalComLiq.add(net);

                continue;
            }
        } else if (emp instanceof EmpregadoAssalariado) {
            if (isAssalariadoPayday) {
                isPayday = true;

                LocalDate periodStart = currentDate.withDayOfMonth(1);
                LocalDate periodEndEx = currentDate.plusDays(1);

                EmpregadoAssalariado a = (EmpregadoAssalariado) emp;
                gross = BigDecimal.valueOf(a.getSalario()).setScale(2, RoundingMode.DOWN);

                if (emp.isSindicalizado() && gross.signum() > 0) {
                    MembroSindicato ms = emp.getMembroSindicato();
                    long days = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEndEx);
                    deductions = deductions.add(BigDecimal.valueOf(ms.getTaxaSindical()).multiply(BigDecimal.valueOf(days)));
                    for (TaxaServico ts : ms.getTaxasDeServico()) {
                        LocalDate d2 = ts.getData();
                        if (!d2.isBefore(periodStart) && d2.isBefore(periodEndEx)) {
                            deductions = deductions.add(BigDecimal.valueOf(ts.getValor()));
                        }
                    }
                }

                BigDecimal net = gross.subtract(deductions).max(BigDecimal.ZERO).setScale(2, RoundingMode.DOWN);
                String metodoStr = metodoPagamentoStr(emp.getMetodoPagamentoObjeto(), emp);

                assalariadosLines.append(String.format(
                        "%-48s %13s %9s %15s %s%n",
                        emp.getNome(),
                        df2.format(gross),
                        df2.format(deductions),
                        df2.format(net),
                        metodoStr
                ));
                totalAssalBruto = totalAssalBruto.add(gross);
                totalAssalDesc  = totalAssalDesc.add(deductions);
                totalAssalLiq   = totalAssalLiq.add(net);
            }
        }
    }

    
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("FOLHA DE PAGAMENTO DO DIA %04d-%02d-%02d%n", currentDate.getYear(), currentDate.getMonthValue(), currentDate.getDayOfMonth()));
    sb.append("====================================\n\n");

    
    sb.append("===============================================================================================================================\n");
    sb.append("===================== HORISTAS ================================================================================================\n");
    sb.append("===============================================================================================================================\n");
    sb.append("Nome                                 Horas Extra Salario Bruto Descontos Salario Liquido Metodo\n");
    sb.append("==================================== ===== ===== ============= ========= =============== ======================================\n");
    sb.append(horistasLines);
    sb.append("\n");
    String hNorm = totalHoristasNormalHours.stripTrailingZeros().toPlainString().replace('.', ',');
    String hExt  = totalHoristasExtraHours.stripTrailingZeros().toPlainString().replace('.', ',');
    sb.append(String.format("%-36s %5s %5s %13s %9s %15s%n%n",
            "TOTAL HORISTAS",
            hNorm, hExt,
            df2.format(totalHoristasBruto),
            df2.format(totalHoristasDescontos),
            df2.format(totalHoristasLiquido)));

    sb.append("===============================================================================================================================\n");
    sb.append("===================== ASSALARIADOS ============================================================================================\n");
    sb.append("===============================================================================================================================\n");
    sb.append("Nome                                             Salario Bruto Descontos Salario Liquido Metodo\n");
    sb.append("================================================ ============= ========= =============== ======================================\n");
    sb.append(assalariadosLines);
        sb.append("\n");

    sb.append(String.format("%-48s %13s %9s %15s%n%n",
            "TOTAL ASSALARIADOS",
            df2.format(totalAssalBruto.setScale(2, RoundingMode.DOWN)),
            df2.format(totalAssalDesc.setScale(2, RoundingMode.DOWN)),
            df2.format(totalAssalLiq.setScale(2, RoundingMode.DOWN))));


    sb.append("===============================================================================================================================\n");
    sb.append("===================== COMISSIONADOS ===========================================================================================\n");
    sb.append("===============================================================================================================================\n");
    sb.append("Nome                  Fixo     Vendas   Comissao Salario Bruto Descontos Salario Liquido Metodo\n");
    sb.append("===================== ======== ======== ======== ============= ========= =============== ======================================\n");
    sb.append(comissionadosLines);
        sb.append("\n");

    sb.append(String.format("%-21s %8s %8s %8s %13s %9s %15s%n", "TOTAL COMISSIONADOS",
            df2.format(totalComFixo.setScale(2, RoundingMode.DOWN)),
            df2.format(totalComVend.setScale(2, RoundingMode.DOWN)),
            df2.format(totalComCom.setScale(2, RoundingMode.DOWN)),
            df2.format(totalComBruto.setScale(2, RoundingMode.DOWN)),
            df2.format(totalComDesc.setScale(2, RoundingMode.DOWN)),
            df2.format(totalComLiq.setScale(2, RoundingMode.DOWN))));

    BigDecimal totalFolha = totalHoristasBruto.add(totalAssalBruto).add(totalComBruto);
    sb.append("\n");
    sb.append(String.format("TOTAL FOLHA: %s%n", df2.format(totalFolha.setScale(2, RoundingMode.DOWN))));

    Path path = java.nio.file.Paths.get(saida);
    String out = sb.toString()
            .replace('\u00A0',' ')
            .replace("\u2028","").replace("\u2029","")
            .replace("\r","");
    java.nio.file.Files.write(path, out.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    commit(__before);
}

    private String metodoPagamentoStr(MetodoPagamento mp, Empregado emp) {
    if (mp instanceof MetodoPagamentoEmMaos) return "Em maos";
    if (mp instanceof MetodoPagamentoBanco) {
        MetodoPagamentoBanco b = (MetodoPagamentoBanco) mp;
        return String.format("%s, Ag. %s CC %s", b.getBanco(), b.getAgencia(), b.getContaCorrente());
    }
    if (mp instanceof MetodoPagamentoCorreios) {
        String end = emp.getEndereco() == null ? "" : emp.getEndereco().trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)$").matcher(end);
        return m.find() ? "Correios, end" + m.group(1) : "Correios";
    }
    return "<>";
}

    public String totalFolha(LocalDate data) {
        Objects.requireNonNull(data, "Data invalida.");

        var symbols = new DecimalFormatSymbols(); symbols.setDecimalSeparator(',');
        var df2 = new DecimalFormat("0.00", symbols); df2.setRoundingMode(RoundingMode.DOWN);

        BigDecimal total = BigDecimal.ZERO;

        for (Empregado emp : this.empregados.values()) {
            String ag = emp.getAgendaPagamento();
            if (ag == null || !isPayday(emp, data)) continue; 

            LocalDate inicioEx = inicioPeriodoExclusivo(ag, data);
            LocalDate inicioIncl = inicioEx.plusDays(1);

            BigDecimal bruto;
            if (emp instanceof EmpregadoHorista) {
                EmpregadoHorista h = (EmpregadoHorista) emp;
                bruto = calcularBrutoHoristaPeriodo(h, inicioIncl, data);
            } else if (emp instanceof EmpregadoComissionado) {
                EmpregadoComissionado c = (EmpregadoComissionado) emp;
                bruto = calcularBrutoComissionadoPeriodo(c, ag, inicioIncl, data);
            } else if (emp instanceof EmpregadoAssalariado) {
                EmpregadoAssalariado a = (EmpregadoAssalariado) emp;
                bruto = calcularBrutoAssalariadoPeriodo(a, ag); 
            } else {
                continue;
            }

            total = total.add(bruto);
        }
        return df2.format(total);
    }

    private BigDecimal calcularBrutoHoristaPeriodo(EmpregadoHorista h, LocalDate iniIncl, LocalDate fimIncl) {
    BigDecimal total = BigDecimal.ZERO;
    for (CartaoDePonto cp : h.cartoesDePonto) {
        LocalDate d = cp.getData();
        if (!d.isBefore(iniIncl) && !d.isAfter(fimIncl)) {
            double horas = cp.getHoras();
            double normal = Math.min(horas, 8.0);
            double extra  = Math.max(0.0, horas - 8.0);
            BigDecimal vh = BigDecimal.valueOf(h.getSalarioPorHora());
            total = total.add(BigDecimal.valueOf(normal).multiply(vh)
                    .add(BigDecimal.valueOf(extra).multiply(vh).multiply(new BigDecimal("1.5"))));
        }
    }
    return total.setScale(2, RoundingMode.DOWN);
}

    private BigDecimal calcularBrutoComissionadoPeriodo(EmpregadoComissionado c, String ag,
                                                        LocalDate iniIncl, LocalDate fimIncl) {
        BigDecimal salario = BigDecimal.valueOf(c.getSalarioMensal());
        BigDecimal fixo;
        java.util.regex.Matcher mS2 = P_SEMANAL_2.matcher(ag);
        java.util.regex.Matcher mS1 = P_SEMANAL_1.matcher(ag);
        if (mS2.matches()) {
            int n = Integer.parseInt(mS2.group(1)); 
            fixo = salario.multiply(BigDecimal.valueOf(12L * n))
                        .divide(BigDecimal.valueOf(52), 2, RoundingMode.DOWN);
        } else if (mS1.matches()) {
            
            fixo = salario.multiply(BigDecimal.valueOf(12))
                        .divide(BigDecimal.valueOf(52), 2, RoundingMode.DOWN);
        } else {
            
            fixo = salario.setScale(2, RoundingMode.DOWN);
        }

        BigDecimal vendas = BigDecimal.ZERO;
        LocalDate fimEx = fimIncl.plusDays(1);
        for (ResultadoDeVenda v : c.getResultadosDeVenda()) {
            LocalDate d = v.getData();
            if (!d.isBefore(iniIncl) && d.isBefore(fimEx)) {
                vendas = vendas.add(BigDecimal.valueOf(v.getValor()));
            }
        }
        BigDecimal comissao = vendas.multiply(BigDecimal.valueOf(c.getTaxaDeComissao()))
                                    .setScale(2, RoundingMode.DOWN);
        return fixo.add(comissao).setScale(2, RoundingMode.DOWN);
    }

    private BigDecimal calcularBrutoAssalariadoPeriodo(EmpregadoAssalariado a, String ag) {
        BigDecimal sal = BigDecimal.valueOf(a.getSalarioMensal());

        java.util.regex.Matcher mS2 = P_SEMANAL_2.matcher(ag);
        java.util.regex.Matcher mS1 = P_SEMANAL_1.matcher(ag);

        if (mS2.matches()) {
            int n = Integer.parseInt(mS2.group(1)); 
            return sal.multiply(BigDecimal.valueOf(12L * n))
                    .divide(BigDecimal.valueOf(52), 2, RoundingMode.DOWN);
        }
        if (mS1.matches()) {
            return sal.multiply(BigDecimal.valueOf(12))
                    .divide(BigDecimal.valueOf(52), 2, RoundingMode.DOWN);
        }
        return sal.setScale(2, RoundingMode.DOWN);
    }

    private boolean isLastBusinessDay(LocalDate d) {
        LocalDate eom = d.withDayOfMonth(d.lengthOfMonth());
        LocalDate lastBiz = eom;
        if (eom.getDayOfWeek() == DayOfWeek.SATURDAY) lastBiz = eom.minusDays(1);
        if (eom.getDayOfWeek() == DayOfWeek.SUNDAY) lastBiz = eom.minusDays(2);
        return d.equals(lastBiz);
    }

    public String getEmpregadoPorNome(String nome, String indice) throws Exception {
        if (nome == null || nome.isEmpty()) {
            throw new NomeNaoPodeSerNuloException();
        }
        int count = 0;

        int indiceInt = Integer.parseInt(indice);
        for (Map.Entry<String, Empregado> entry : empregados.entrySet()) {
            if (nome.equals(entry.getValue().getNome())) {
                count++;
                if (count == indiceInt) {
                    return entry.getKey();
                }
            }
        }
        throw new NaoHaEmpregadoComEsseNomeException();
    }
    
    public String getHorasNormaisTrabalhadas(String empId, String dataInicial, String dataFinal) throws Exception {
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        Empregado empregado = this.empregados.get(empId);
        if (!(empregado instanceof EmpregadoHorista)) {
            throw new EmpregadoNaoEhHoristaException();
        }

        LocalDate inicio, fim;
        try {
            inicio = parseDateFlexible(dataInicial);
        } catch (DateTimeParseException e) {
            throw new DataInicialInvalidaException();
        }
        try {
            fim = parseDateFlexible(dataFinal);
        } catch (DateTimeParseException e) {
            throw new DataFinalInvalidaException();
        }

        if (inicio.isAfter(fim)) {
            throw new DataInicialPosteriorFinalException();
        }

        EmpregadoHorista horista = (EmpregadoHorista) empregado;
        Map<LocalDate, Double> horasPorDia = new HashMap<>();
        for (CartaoDePonto cartao : horista.cartoesDePonto) {
            LocalDate data = cartao.getData();
            if ((data.isEqual(inicio) || data.isAfter(inicio)) && data.isBefore(fim)) {
                horasPorDia.put(data, horasPorDia.getOrDefault(data, 0.0) + cartao.getHoras());
            }
        }
        double totalNormais = 0.0;
        for (double horasDia : horasPorDia.values()) {
            totalNormais += Math.min(horasDia, 8);
        }
        if (totalNormais == Math.floor(totalNormais)) {
            return String.valueOf((int) totalNormais);
        }
        return String.format("%.1f", totalNormais).replace('.', ',');
    }

    public String getHorasExtrasTrabalhadas(String empId, String dataInicial, String dataFinal) throws Exception {
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        Empregado empregado = this.empregados.get(empId);
        if (!(empregado instanceof EmpregadoHorista)) {
            throw new EmpregadoNaoEhHoristaException();
        }

        LocalDate inicio, fim;
    try {
        inicio = parseDateFlexible(dataInicial);
    } catch (DateTimeParseException e) {
        throw new DataInicialInvalidaException();
    }
    try {
        fim = parseDateFlexible(dataFinal);
    } catch (DateTimeParseException e) {
        throw new DataFinalInvalidaException();
    }

    if (inicio.isAfter(fim)) {
        throw new DataInicialPosteriorFinalException();
    }

        EmpregadoHorista horista = (EmpregadoHorista) empregado;
        Map<LocalDate, Double> horasPorDia = new HashMap<>();
        for (CartaoDePonto cartao : horista.cartoesDePonto) {
            LocalDate data = cartao.getData();
            if ((data.isEqual(inicio) || data.isAfter(inicio)) && data.isBefore(fim)) {
                horasPorDia.put(data, horasPorDia.getOrDefault(data, 0.0) + cartao.getHoras());
            }
        }
        double totalExtras = 0.0;
        for (double horasDia : horasPorDia.values()) {
            totalExtras += horasDia > 8 ? horasDia - 8 : 0; 
        }
        if (totalExtras == Math.floor(totalExtras)) {
            return String.valueOf((int) totalExtras);
        }
        return String.format("%.1f", totalExtras).replace('.', ',');
    }

    public String getVendasRealizadas(String empId, String dataInicial, String dataFinal) throws Exception {
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        Empregado empregado = this.empregados.get(empId);
        if (!(empregado instanceof EmpregadoComissionado)) {
            throw new EmpregadoNaoEhComissionadoException();
        }

        
        LocalDate inicio, fim;
        try {
            inicio = parseDateFlexible(dataInicial);
        } catch (DateTimeParseException e) {
            throw new DataInicialInvalidaException();
        }
        try {
            fim = parseDateFlexible(dataFinal);
        } catch (DateTimeParseException e) {
            throw new DataFinalInvalidaException();
        }
        if (inicio.isAfter(fim)) {
            throw new DataInicialPosteriorFinalException();
        }

        EmpregadoComissionado comissionado = (EmpregadoComissionado) empregado;
        double totalVendas = 0.0;
        for (ResultadoDeVenda venda : comissionado.getResultadosDeVenda()) {
            LocalDate data = venda.getData();
            if ((data.isEqual(inicio) || data.isAfter(inicio)) && data.isBefore(fim)) {
                totalVendas += venda.getValor();
            }
        }
        return String.format("%.2f", totalVendas).replace('.', ',');
    }

    public String getTaxasServico(String empId, String dataInicial, String dataFinal) throws Exception {
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        Empregado empregado = this.empregados.get(empId);
        if (!empregado.isSindicalizado()) {
            throw new EmpregadoNaoEhSindicalizadoException();
        }

        LocalDate inicio, fim;
        try {
            inicio = parseDateFlexible(dataInicial);;
        } catch (DateTimeParseException e) {
            throw new DataInicialInvalidaException();
        }
        try {
            fim = parseDateFlexible(dataFinal);;
        } catch (DateTimeParseException e) {
            throw new DataFinalInvalidaException();
        }
        if (inicio.isAfter(fim)) {
            throw new DataInicialPosteriorFinalException();
        }

        MembroSindicato sindicato = empregado.getMembroSindicato();
        double totalTaxas = 0.0;
        for (TaxaServico taxa : sindicato.getTaxasDeServico()) {
            LocalDate data = taxa.getData();
            if ((data.isEqual(inicio) || data.isAfter(inicio)) && data.isBefore(fim)) {
                totalTaxas += taxa.getValor();
            }
        }
        return String.format("%.2f", totalTaxas).replace('.', ',');
    }

    private LocalDate parseDateFlexible(String dateStr) throws DateTimeParseException {
        if (dateStr == null) {
            throw new DateTimeParseException("Date is null", dateStr, 0);
        }
        String s = dateStr.trim();
        String[] parts = s.split("/");
        if (parts.length != 3) {
            throw new DateTimeParseException("Invalid date format", dateStr, 0);
        }
        try {
            int day = Integer.parseInt(parts[0].trim());
            int month = Integer.parseInt(parts[1].trim());
            int year = Integer.parseInt(parts[2].trim());
            if (month < 1 || month > 12) {
                throw new DateTimeParseException("Invalid month", dateStr, 0);
            }
            YearMonth ym = YearMonth.of(year, month);
            int maxDay = ym.lengthOfMonth();
            if (day < 1 || day > maxDay) {
                throw new DateTimeParseException("Invalid day", dateStr, 0);
            }
            return LocalDate.of(year, month, day);
        } catch (NumberFormatException e) {
            throw new DateTimeParseException("Invalid date", dateStr, 0);
        } catch (DateTimeException e) {
            throw new DateTimeParseException("Invalid date", dateStr, 0);
        }
    }

    private boolean isPayday(Empregado emp, java.time.LocalDate data) {
        String ag = emp.getAgendaPagamento();
        if (ag == null || ag.isBlank()) return false;

        if ("mensal $".equals(ag)) return isLastBusinessDay(data);

        java.util.regex.Matcher mMensal = P_MENSAL_D.matcher(ag);
        if (mMensal.matches()) {
            int dia = Integer.parseInt(mMensal.group(1));
            return data.getDayOfMonth() == dia;
        }

        java.util.regex.Matcher mS1 = P_SEMANAL_1.matcher(ag);
        if (mS1.matches()) {
            int dow = Integer.parseInt(mS1.group(1));
            return data.getDayOfWeek().getValue() == dow;
        }

        java.util.regex.Matcher mS2 = P_SEMANAL_2.matcher(ag);
        if (mS2.matches()) {
            int n   = Integer.parseInt(mS2.group(1));
            int dow = Integer.parseInt(mS2.group(2)); 
            if (data.getDayOfWeek().getValue() != dow) return false;
            int week = data.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
            return week % n == 0;
        }

        return false;
    }

    private java.time.LocalDate inicioPeriodoExclusivo(String agenda, java.time.LocalDate data) {
        if ("mensal $".equals(agenda)) {
            return data.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth()).minusDays(1);
        }
        java.util.regex.Matcher mMensal = P_MENSAL_D.matcher(agenda);
        if (mMensal.matches()) {
            return data.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth()).minusDays(1);
        }
        java.util.regex.Matcher mS1 = P_SEMANAL_1.matcher(agenda);
        if (mS1.matches()) {
            return data.minusDays(7); 
        }
        java.util.regex.Matcher mS2 = P_SEMANAL_2.matcher(agenda);
        if (mS2.matches()) {
            int n = Integer.parseInt(mS2.group(1));
            return data.minusDays(n * 7L); 
        }
        return data;
    }

}
