package common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Uma linha do historico de um posto (tabela historico). */
public class EntradaHistorico implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int postoId;
    private Integer reservaId;      // Integer (e nao int) porque pode ser null
    private String tipo;            // REGISTO, APROVACAO, RESERVA_CRIADA, ...
    private String estadoAnterior;  // pode ser null
    private String estadoNovo;
    private String descricao;
    private LocalDateTime dataHora;

    public EntradaHistorico(int id, int postoId, Integer reservaId, String tipo,
                            String estadoAnterior, String estadoNovo,
                            String descricao, LocalDateTime dataHora) {
        this.id = id;
        this.postoId = postoId;
        this.reservaId = reservaId;
        this.tipo = tipo;
        this.estadoAnterior = estadoAnterior;
        this.estadoNovo = estadoNovo;
        this.descricao = descricao;
        this.dataHora = dataHora;
    }

    public int getId()                { return id; }
    public int getPostoId()           { return postoId; }
    public Integer getReservaId()     { return reservaId; }
    public String getTipo()           { return tipo; }
    public String getEstadoAnterior() { return estadoAnterior; }
    public String getEstadoNovo()     { return estadoNovo; }
    public String getDescricao()      { return descricao; }
    public LocalDateTime getDataHora(){ return dataHora; }

    @Override
    public String toString() {
        String mudanca = (estadoAnterior == null ? "-" : estadoAnterior) + " -> " + estadoNovo;
        String data = dataHora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return String.format("%s | %-18s | %-27s | %s", data, tipo, mudanca, descricao);
    }
}
