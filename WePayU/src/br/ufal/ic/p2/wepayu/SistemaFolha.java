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
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class SistemaFolha implements Serializable, Cloneable {

    private Map<String, Empregado> empregados;
    //private Stack<Map<String, Empregado>> historicoEstados = new Stack<>();
    //private static final String SISTEMA_FILE = "wepayu.dat";

    public SistemaFolha() {
        this.empregados = new HashMap<>();
    }

    public void zerarSistema() {
        this.empregados.clear();
    }

    public void addEmpregado(String id, String nome, String endereco, String tipo, String salario, String comissao) throws Exception {
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
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }   
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
    this.empregados.remove(empId);
    }

    public void lancaCartao(String empId, String data, String horas) throws Exception {
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
    }

    public void lancaVenda(String empId, String data, String valor) throws Exception {
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
    }

    public void lancaTaxaServico(String sindId, String data, String valor) throws Exception {
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
    }

    public void undo() throws Exception {
        // fazer
    }

    public void redo() throws Exception {
        // fazer
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

    public void rodaFolha(String data, String saida) throws Exception {

    if (data == null || data.trim().isEmpty()) throw new DataInvalidaException();
    LocalDate currentDate;
    try {
        currentDate = parseDateFlexible(data);
    } catch (DateTimeParseException e) {
        throw new DataInvalidaException();
    }

    LocalDate comissionadoFirstPay = LocalDate.of(2005, 1, 14);

    java.util.List<Empregado> _emps = new java.util.ArrayList<>();
    for (Map.Entry<String, Empregado> e : this.empregados.entrySet()) _emps.add(e.getValue());
    _emps.sort((a,b) -> a.getNome().compareToIgnoreCase(b.getNome()));

    StringBuilder horistasLines = new StringBuilder();
    BigDecimal totalHoristasNormalHours = BigDecimal.ZERO;
    BigDecimal totalHoristasExtraHours = BigDecimal.ZERO;
    BigDecimal totalHoristasBruto = BigDecimal.ZERO;
    BigDecimal totalHoristasDescontos = BigDecimal.ZERO;
    BigDecimal totalHoristasLiquido = BigDecimal.ZERO;

    StringBuilder assalariadosLines = new StringBuilder();
    BigDecimal totalAssalariadosBruto = BigDecimal.ZERO;
    BigDecimal totalAssalariadosDescontos = BigDecimal.ZERO;
    BigDecimal totalAssalariadosLiquido = BigDecimal.ZERO;

    StringBuilder comissionadosLines = new StringBuilder();
    BigDecimal totalComissionadosBruto = BigDecimal.ZERO;
    BigDecimal totalComissionadosDescontos = BigDecimal.ZERO;
    BigDecimal totalComissionadosLiquido = BigDecimal.ZERO;

    java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols();
    sym.setDecimalSeparator(',');
    java.text.DecimalFormat df2 = new java.text.DecimalFormat("0.00");
    df2.setDecimalFormatSymbols(sym);
    java.text.DecimalFormat df1 = new java.text.DecimalFormat("0.0");
    df1.setDecimalFormatSymbols(sym);

    boolean isHoristaPayday = currentDate.getDayOfWeek() == DayOfWeek.FRIDAY;
    boolean isAssalariadoPayday = currentDate.equals(currentDate.withDayOfMonth(currentDate.lengthOfMonth()));
    boolean isComissionadoPayday = false;
    if (!currentDate.isBefore(comissionadoFirstPay)) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(comissionadoFirstPay, currentDate);
        if (currentDate.getDayOfWeek() == java.time.DayOfWeek.FRIDAY && days % 14 == 0) isComissionadoPayday = true;
    }


    for (Empregado emp : _emps) {

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;
        double normalHours = 0.0;
        double extraHours = 0.0;
        boolean isPayday = false;


        if (emp instanceof EmpregadoHorista) {
            if (isHoristaPayday) {
                isPayday = true;
                LocalDate periodEnd = currentDate;
                LocalDate periodStart = currentDate.minusDays(6);
                LocalDate periodEndExclusive = periodEnd.plusDays(1);

                EmpregadoHorista h = (EmpregadoHorista) emp;
                BigDecimal hourly = BigDecimal.valueOf(h.getSalario());
                java.util.Map<LocalDate, Double> hoursByDay = new java.util.HashMap<>();
                for (CartaoDePonto c : h.cartoesDePonto) {
                    LocalDate d2 = c.getData();
                    if ((d2.isEqual(periodStart) || d2.isAfter(periodStart)) && d2.isBefore(periodEndExclusive)) {
                        hoursByDay.put(d2, hoursByDay.getOrDefault(d2, 0.0) + c.getHoras());
                    }
                }
                for (Double hrs : hoursByDay.values()) {
                    normalHours += Math.min(hrs, 8.0);
                    if (hrs > 8.0) extraHours += (hrs - 8.0);
                }
                BigDecimal normalHoursBD = BigDecimal.valueOf(normalHours);
                BigDecimal extraHoursBD = BigDecimal.valueOf(extraHours);
                gross = normalHoursBD.multiply(hourly).add(extraHoursBD.multiply(hourly).multiply(BigDecimal.valueOf(1.5)));
                gross = gross.setScale(2, RoundingMode.DOWN);

                if (emp.isSindicalizado()) {
                    if (gross.compareTo(BigDecimal.ZERO) > 0) {
                        MembroSindicato ms = emp.getMembroSindicato();
                        BigDecimal taxaDiaria = BigDecimal.valueOf(ms.getTaxaSindical());
                        long daysBetween;
                        java.time.LocalDate lastPay = null;
                        java.time.LocalDate probe = periodEnd.minusDays(7);
                        while (probe.isAfter(java.time.LocalDate.of(1900,1,1))) {
                            java.time.LocalDate pStart = probe.minusDays(6);
                            java.time.LocalDate pEndEx = probe.plusDays(1);
                            double hourlyDouble = ((EmpregadoHorista) emp).getSalario();
                            java.util.Map<java.time.LocalDate, Double> hrs = new java.util.HashMap<>();
                            for (CartaoDePonto cp : ((EmpregadoHorista) emp).cartoesDePonto) {
                                java.time.LocalDate d2 = cp.getData();
                                if ((d2.isEqual(pStart) || d2.isAfter(pStart)) && d2.isBefore(pEndEx)) {
                                    hrs.put(d2, hrs.getOrDefault(d2, 0.0) + cp.getHoras());
                                }
                            }
                            double normalPrev = 0.0, extraPrev = 0.0;
                            for (Double e : hrs.values()) {
                                normalPrev += Math.min(e, 8.0);
                                if (e > 8.0) extraPrev += (e - 8.0);
                            }
                            double grossPrev = (normalPrev * hourlyDouble) + (extraPrev * hourlyDouble * 1.5);
                            if (grossPrev > 0.0) {
                                lastPay = probe;
                                break;
                            }
                            probe = probe.minusDays(7);
                        }
                        if (lastPay == null) {
                            
                            daysBetween = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEndExclusive);
                        } else {
                            daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastPay.plusDays(1), periodEndExclusive);
                        }
                        deductions = deductions.add(taxaDiaria.multiply(BigDecimal.valueOf(daysBetween)));
                        for (TaxaServico ts : ms.getTaxasDeServico()) {
                            LocalDate d2 = ts.getData();
                            if ((d2.isEqual(periodStart) || d2.isAfter(periodStart)) && d2.isBefore(periodEndExclusive)) {
                                deductions = deductions.add(BigDecimal.valueOf(ts.getValor()));
                            }
                        }
                    }
                }
            }
        } else if (emp instanceof EmpregadoComissionado) {
            if(isComissionadoPayday) {
                isPayday = true;
                LocalDate periodEnd = currentDate;
                LocalDate periodStart = currentDate.minusDays(13);
                LocalDate periodEndExclusive = periodEnd.plusDays(1);

                EmpregadoComissionado c = (EmpregadoComissionado) emp;
                BigDecimal salarioBD = BigDecimal.valueOf(c.getSalario());
                BigDecimal base = salarioBD.multiply(BigDecimal.valueOf(12)).divide(BigDecimal.valueOf(26), 2, RoundingMode.DOWN);
                BigDecimal commissions = BigDecimal.ZERO;
                for (ResultadoDeVenda v : c.getResultadosDeVenda()) {
                    LocalDate d2 = v.getData();
                    if ((d2.isEqual(periodStart) || d2.isAfter(periodStart)) && d2.isBefore(periodEndExclusive)) {
                        commissions = commissions.add(BigDecimal.valueOf(v.getValor()).multiply(BigDecimal.valueOf(c.getTaxaDeComissao())));
                    }
                }
                commissions = commissions.setScale(2, RoundingMode.DOWN);
                gross = base.add(commissions);
                gross = gross.setScale(2, RoundingMode.DOWN);
                
                 if (emp.isSindicalizado()) {
                    if (gross.compareTo(BigDecimal.ZERO) > 0) {
                        MembroSindicato ms = emp.getMembroSindicato();
                        BigDecimal taxaDiaria = BigDecimal.valueOf(ms.getTaxaSindical());
                        long daysBetween;
                        java.time.LocalDate lastPay = null;
                        java.time.LocalDate probe = periodEnd.minusDays(14);
                        while (probe.isAfter(java.time.LocalDate.of(1900,1,1))) {
                             java.time.LocalDate pStart = probe.minusDays(13);
                             java.time.LocalDate pEndEx = probe.plusDays(1);
                            double comissaoDouble = c.getTaxaDeComissao();
                            double salarioDouble = c.getSalario();
                            BigDecimal basePrev = BigDecimal.valueOf(salarioDouble).multiply(BigDecimal.valueOf(12)).divide(BigDecimal.valueOf(26), 2, RoundingMode.DOWN);
                            BigDecimal commissionsPrev = BigDecimal.ZERO;
                            for (ResultadoDeVenda v : c.getResultadosDeVenda()) {
                                LocalDate d2 = v.getData();
                                if ((d2.isEqual(pStart) || d2.isAfter(pStart)) && d2.isBefore(pEndEx)) {
                                    commissionsPrev = commissionsPrev.add(BigDecimal.valueOf(v.getValor()).multiply(BigDecimal.valueOf(comissaoDouble)));
                                }
                            }
                            commissionsPrev = commissionsPrev.setScale(2, RoundingMode.DOWN);
                            BigDecimal grossPrev = basePrev.add(commissionsPrev);
                            grossPrev = grossPrev.setScale(2, RoundingMode.DOWN);
                            if (grossPrev.compareTo(BigDecimal.ZERO) > 0) {
                                lastPay = probe;
                                break;
                            }
                            probe = probe.minusDays(14);
                        }
                        
                         if (lastPay == null) {
                        
                            daysBetween = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEndExclusive);
                        } else {
                            daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastPay.plusDays(1), periodEndExclusive);
                        }
                        deductions = deductions.add(taxaDiaria.multiply(BigDecimal.valueOf(daysBetween)));
                        for (TaxaServico ts : ms.getTaxasDeServico()) {
                            LocalDate d2 = ts.getData();
                            if ((d2.isEqual(periodStart) || d2.isAfter(periodStart)) && d2.isBefore(periodEndExclusive)) {
                                deductions = deductions.add(BigDecimal.valueOf(ts.getValor()));
                            }
                        }
                    }
                }
            }
        } else if (emp instanceof EmpregadoAssalariado) {
            if(isAssalariadoPayday) {
                isPayday = true;
                LocalDate periodEnd = currentDate;
                LocalDate periodStart = currentDate.withDayOfMonth(1);
                LocalDate periodEndExclusive = periodEnd.plusDays(1);
                
                EmpregadoAssalariado a = (EmpregadoAssalariado) emp;
                gross = BigDecimal.valueOf(a.getSalario());
                gross = gross.setScale(2, RoundingMode.DOWN);

                if (emp.isSindicalizado()) {
                    if (gross.compareTo(BigDecimal.ZERO) > 0) {
                        MembroSindicato ms = emp.getMembroSindicato();
                        BigDecimal taxaDiaria = BigDecimal.valueOf(ms.getTaxaSindical());
                        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEndExclusive);
                        deductions = deductions.add(taxaDiaria.multiply(BigDecimal.valueOf(daysBetween)));
                        for (TaxaServico ts : ms.getTaxasDeServico()) {
                            LocalDate d2 = ts.getData();
                            if ((d2.isEqual(periodStart) || d2.isAfter(periodStart)) && d2.isBefore(periodEndExclusive)) {
                                deductions = deductions.add(BigDecimal.valueOf(ts.getValor()));
                            }
                        }
                    }
                }
            }
        }

        if(!isPayday) continue;
        
        deductions = deductions.setScale(2, RoundingMode.DOWN);
        BigDecimal net = gross.subtract(deductions);
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;
        net = net.setScale(2, RoundingMode.DOWN);

        String metodoStr = "";
        MetodoPagamento mp = emp.getMetodoPagamentoObjeto();
        if (mp instanceof MetodoPagamentoEmMaos) {
            metodoStr = "Em maos";
        } else if (mp instanceof MetodoPagamentoBanco) {
            MetodoPagamentoBanco mb = (MetodoPagamentoBanco) mp;
            metodoStr = String.format("%s, Ag. %s CC %s", mb.getBanco(), mb.getAgencia(), mb.getContaCorrente());
        } else if (mp instanceof MetodoPagamentoCorreios) {
            metodoStr = "Correios";
        }

        if (emp instanceof EmpregadoHorista) {
            String horasNormaisStr = normalHours == Math.floor(normalHours) ? String.valueOf((int) normalHours) : df1.format(normalHours);
            String horasExtrasStr = extraHours == Math.floor(extraHours) ? String.valueOf((int) extraHours) : df1.format(extraHours);
            String line = String.format("%-36s %5s %5s %13s %9s %15s %s",
                    emp.getNome(),
                    horasNormaisStr,
                    horasExtrasStr,
                    df2.format(gross),
                    df2.format(deductions),
                    df2.format(net),
                    metodoStr);
            horistasLines.append(line).append("\n");

            totalHoristasNormalHours = totalHoristasNormalHours.add(BigDecimal.valueOf(normalHours));
            totalHoristasExtraHours = totalHoristasExtraHours.add(BigDecimal.valueOf(extraHours));
            totalHoristasBruto = totalHoristasBruto.add(gross);
            totalHoristasDescontos = totalHoristasDescontos.add(deductions);
            totalHoristasLiquido = totalHoristasLiquido.add(net);
        } else if (emp instanceof EmpregadoAssalariado) {
            String line = String.format("%-45s %13s %9s %15s %s",
                    emp.getNome(),
                    df2.format(gross),
                    df2.format(deductions),
                    df2.format(net),
                    metodoStr);
            assalariadosLines.append(line).append("\n");

            totalAssalariadosBruto = totalAssalariadosBruto.add(gross);
            totalAssalariadosDescontos = totalAssalariadosDescontos.add(deductions);
            totalAssalariadosLiquido = totalAssalariadosLiquido.add(net);
        } else if (emp instanceof EmpregadoComissionado) {
            String line = String.format("%-21s %7s %8s %8s %12s %8s %14s %s",
                    emp.getNome(),
                    df2.format(BigDecimal.ZERO),
                    df2.format(BigDecimal.ZERO),
                    df2.format(BigDecimal.ZERO),
                    df2.format(gross),
                    df2.format(deductions),
                    df2.format(net),
                    metodoStr);
            comissionadosLines.append(line).append("\n");

            totalComissionadosBruto = totalComissionadosBruto.add(gross);
            totalComissionadosDescontos = totalComissionadosDescontos.add(deductions);
            totalComissionadosLiquido = totalComissionadosLiquido.add(net);
        }

        
    }
    

    StringBuilder sb = new StringBuilder();
    sb.append(String.format("FOLHA DE PAGAMENTO DO DIA %04d-%02d-%02d\n", currentDate.getYear(), currentDate.getMonthValue(), currentDate.getDayOfMonth()));
    sb.append("====================================\n\n");
    sb.append("===============================================================================================================================\n");
    sb.append("===================== HORISTAS ================================================================================================\n");
    sb.append("===============================================================================================================================\n");
    sb.append("Nome                                 Horas Extra Salario Bruto Descontos Salario Liquido Metodo\n");
    sb.append("==================================== ===== ===== ============= ========= =============== ======================================\n");
    sb.append(horistasLines.toString());
    sb.append("\n");
    sb.append(String.format("TOTAL HORISTAS                          %5s %5s        %s     %s          %s\n",
            totalHoristasNormalHours.stripTrailingZeros().toPlainString().replace('.', ','),
            totalHoristasExtraHours.stripTrailingZeros().toPlainString().replace('.', ','),
            df2.format(totalHoristasBruto.setScale(2, RoundingMode.DOWN)),
            df2.format(totalHoristasDescontos.setScale(2, RoundingMode.DOWN)),
            df2.format(totalHoristasLiquido.setScale(2, RoundingMode.DOWN))
    ));
    sb.append("\n\n");

    sb.append("===============================================================================================================================\n");
    sb.append("===================== ASSALARIADOS ============================================================================================\n");
    sb.append("===============================================================================================================================\n");
    sb.append("Nome                                             Salario Bruto Descontos Salario Liquido Metodo\n");
    sb.append("================================================ ============= ========= =============== ======================================\n\n");
    sb.append(assalariadosLines.toString());
    sb.append("\n");
    sb.append(String.format("TOTAL ASSALARIADOS                                        %s      %s            %s",
            df2.format(totalAssalariadosBruto.setScale(2, RoundingMode.DOWN)),
            df2.format(totalAssalariadosDescontos.setScale(2, RoundingMode.DOWN)),
            df2.format(totalAssalariadosLiquido.setScale(2, RoundingMode.DOWN))
    ));
    sb.append("\n\n");

    sb.append("===============================================================================================================================\n");
    sb.append("===================== COMISSIONADOS ===========================================================================================\n");
    sb.append("===============================================================================================================================\n");
    sb.append("Nome                  Fixo     Vendas   Comissao Salario Bruto Descontos Salario Liquido Metodo\n");
    sb.append("===================== ======== ======== ======== ============= ========= =============== ======================================\n\n");
    sb.append(comissionadosLines.toString());
    sb.append("\n");
    sb.append(String.format("TOTAL COMISSIONADOS       %s     %s     %s          %s      %s            %s\n",
            df2.format(BigDecimal.ZERO),
            df2.format(BigDecimal.ZERO),
            df2.format(BigDecimal.ZERO),
            df2.format(totalComissionadosBruto.setScale(2, RoundingMode.DOWN)),
            df2.format(totalComissionadosDescontos.setScale(2, RoundingMode.DOWN)),
            df2.format(totalComissionadosLiquido.setScale(2, RoundingMode.DOWN))));

    BigDecimal totalFolha = totalHoristasBruto.add(totalAssalariadosBruto).add(totalComissionadosBruto);
    sb.append(String.format("TOTAL FOLHA: %s\n", df2.format(totalFolha.setScale(2, RoundingMode.DOWN))));

    java.nio.file.Files.write(java.nio.file.Paths.get(saida), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    
    public String totalFolha(LocalDate data) {
        Objects.requireNonNull(data, "Data invalida.");
        
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        DecimalFormat df2 = new DecimalFormat("0.00", symbols);
        df2.setRoundingMode(RoundingMode.DOWN);

        BigDecimal total = BigDecimal.ZERO;

        if (isEndOfMonth(data)) {
            total = total.add(new BigDecimal(sumBrutoAssalariados().replace(',', '.')));
        }

        if (isComissionadoPayday(data)) {
            LocalDate inicioPeriodo = data.minusDays(13);
            total = total.add(new BigDecimal(sumBrutoComissionados(inicioPeriodo, data).replace(',', '.')));
        }

        if (data.getDayOfWeek() == DayOfWeek.FRIDAY) {
            LocalDate inicioSemana = data.minusDays(6); 
            total = total.add(new BigDecimal(sumBrutoHoristas(inicioSemana, data).replace(',', '.')));
        }
        
        return df2.format(total);
    }


    private boolean isEndOfMonth(LocalDate d) {
        return d.equals(d.withDayOfMonth(d.lengthOfMonth()));
    }


    private boolean isComissionadoPayday(LocalDate d) {
        if (d.getDayOfWeek() != DayOfWeek.FRIDAY) return false;
        LocalDate base = LocalDate.of(2005, 1, 14); 
        long days = ChronoUnit.DAYS.between(base, d);
        return days % 14 == 0;
    }

    private String sumBrutoHoristas(LocalDate inicio, LocalDate fim) {
        BigDecimal total = BigDecimal.ZERO;
        for (Empregado e : this.empregados.values()) { 
            if (e instanceof EmpregadoHorista) {
                EmpregadoHorista h = (EmpregadoHorista) e;
                BigDecimal brutoSemana = calcularBrutoHorista(h, inicio, fim);
                total = total.add(brutoSemana);
            }
        }
        return total.setScale(2, RoundingMode.DOWN).toString().replace('.', ',');
    }


    private String sumBrutoComissionados(LocalDate inicio, LocalDate fim) {
        BigDecimal total = BigDecimal.ZERO;
        for (Empregado e : this.empregados.values()) { 
            if (e instanceof EmpregadoComissionado) {
                EmpregadoComissionado c = (EmpregadoComissionado) e;
                BigDecimal salarioBD = new BigDecimal(String.valueOf(c.getSalario()));
                BigDecimal fixoQuinzenal = salarioBD.multiply(BigDecimal.valueOf(12)).divide(BigDecimal.valueOf(26), 2, RoundingMode.DOWN);
                BigDecimal comissao = BigDecimal.valueOf(somarVendas(c, inicio, fim)).multiply(BigDecimal.valueOf(c.getTaxaDeComissao())).setScale(2, RoundingMode.DOWN);
                total = total.add(fixoQuinzenal).add(comissao);
            }
        }
        return total.setScale(2, RoundingMode.DOWN).toString().replace('.', ',');
    }

    private String sumBrutoAssalariados() {
        BigDecimal total = BigDecimal.ZERO;
        for (Empregado e : this.empregados.values()) { 
            if (e instanceof EmpregadoAssalariado && !(e instanceof EmpregadoComissionado)) {
                EmpregadoAssalariado a = (EmpregadoAssalariado) e;
                total = total.add(BigDecimal.valueOf(a.getSalarioMensal()));
            }
        }
        return total.setScale(2, RoundingMode.DOWN).toString().replace('.', ',');
    }

    private BigDecimal calcularBrutoHorista(EmpregadoHorista h, LocalDate inicio, LocalDate fim) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartaoDePonto cp : h.cartoesDePonto) {
            LocalDate d = cp.getData();
            if (!d.isBefore(inicio) && !d.isAfter(fim)) {
                double horas  = cp.getHoras();
                double normal = Math.min(horas, 8.0);
                double extra  = Math.max(0.0, horas - 8.0);
                BigDecimal vh = BigDecimal.valueOf(h.getSalarioPorHora());     
                BigDecimal normalBD = BigDecimal.valueOf(normal);
                BigDecimal extraBD = BigDecimal.valueOf(extra);
                
                total = total.add(normalBD.multiply(vh).add(extraBD.multiply(vh).multiply(new BigDecimal("1.5"))));
            }
        }
        return total.setScale(2, RoundingMode.DOWN);
    }

    private double somarVendas(EmpregadoComissionado c, LocalDate inicio, LocalDate fim) {
        double total = 0.0;
        for (ResultadoDeVenda v : c.getResultadosDeVenda()) {
            LocalDate d = v.getData();
            if (!d.isBefore(inicio) && !d.isAfter(fim)) {
                total += v.getValor();
            }
        }
        return total;
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
}
