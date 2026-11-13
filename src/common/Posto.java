package common;

import java.io.Serializable;

/** Um posto de carregamento (o elemento gerido; tabela posto). */
public class Posto implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String codigo;
    private String localizacao;
    private String cidade;
    private double potenciaKw;
    private TipoFicha tipoFicha;
    private double precoKwh;
    private EstadoPosto estado;
    private EstadoAdmin estadoAdmin;

    /** Construtor para um posto NOVO: comeca DISPONIVEL e PENDENTE. */
    public Posto(String codigo, String localizacao, String cidade,
                 double potenciaKw, TipoFicha tipoFicha, double precoKwh) {
        this(0, codigo, localizacao, cidade, potenciaKw, tipoFicha, precoKwh,
             EstadoPosto.DISPONIVEL, EstadoAdmin.PENDENTE);
    }

    /** Construtor completo (usado quando se le da BD). */
    public Posto(int id, String codigo, String localizacao, String cidade,
                 double potenciaKw, TipoFicha tipoFicha, double precoKwh,
                 EstadoPosto estado, EstadoAdmin estadoAdmin) {
        this.id = id;
        this.codigo = codigo;
        this.localizacao = localizacao;
        this.cidade = cidade;
        this.potenciaKw = potenciaKw;
        this.tipoFicha = tipoFicha;
        this.precoKwh = precoKwh;
        this.estado = estado;
        this.estadoAdmin = estadoAdmin;
    }

    // --- getters ---
    public int getId()                 { return id; }
    public String getCodigo()          { return codigo; }
    public String getLocalizacao()     { return localizacao; }
    public String getCidade()          { return cidade; }
    public double getPotenciaKw()      { return potenciaKw; }
    public TipoFicha getTipoFicha()    { return tipoFicha; }
    public double getPrecoKwh()        { return precoKwh; }
    public EstadoPosto getEstado()     { return estado; }
    public EstadoAdmin getEstadoAdmin(){ return estadoAdmin; }

    // --- setters (so para os atributos que o admin pode editar) ---
    public void setCodigo(String codigo)           { this.codigo = codigo; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }
    public void setCidade(String cidade)           { this.cidade = cidade; }
    public void setPotenciaKw(double potenciaKw)   { this.potenciaKw = potenciaKw; }
    public void setTipoFicha(TipoFicha tipoFicha)  { this.tipoFicha = tipoFicha; }
    public void setPrecoKwh(double precoKwh)       { this.precoKwh = precoKwh; }

    @Override
    public String toString() {
        return String.format("#%d %s | %s, %s | %.0f kW | %s | %.3f EUR/kWh | %s | %s",
                id, codigo, localizacao, cidade, potenciaKw, tipoFicha, precoKwh,
                estado, estadoAdmin);
    }
}
