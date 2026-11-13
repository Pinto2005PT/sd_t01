package common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Uma reserva de carregamento (a operacao principal; tabela reserva). */
public class Reserva implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int utilizadorId;
    private int postoId;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private EstadoReserva estado;
    private LocalDateTime criadaEm;

    // Campos extra, so preenchidos na funcionalidade "as minhas reservas"
    private String codigoPosto;      // ex.: "EVR-001"
    private double custoEstimado;    // em euros

    /** Construtor completo (usado quando se le da BD). */
    public Reserva(int id, int utilizadorId, int postoId,
                   LocalDateTime dataInicio, LocalDateTime dataFim,
                   EstadoReserva estado, LocalDateTime criadaEm) {
        this.id = id;
        this.utilizadorId = utilizadorId;
        this.postoId = postoId;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.estado = estado;
        this.criadaEm = criadaEm;
    }

    public int getId()                  { return id; }
    public int getUtilizadorId()        { return utilizadorId; }
    public int getPostoId()             { return postoId; }
    public LocalDateTime getDataInicio(){ return dataInicio; }
    public LocalDateTime getDataFim()   { return dataFim; }
    public EstadoReserva getEstado()    { return estado; }
    public LocalDateTime getCriadaEm()  { return criadaEm; }

    public String getCodigoPosto()      { return codigoPosto; }
    public double getCustoEstimado()    { return custoEstimado; }
    public void setCodigoPosto(String codigoPosto)     { this.codigoPosto = codigoPosto; }
    public void setCustoEstimado(double custoEstimado) { this.custoEstimado = custoEstimado; }

    @Override
    public String toString() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String texto = String.format("Reserva #%d | utilizador %d | posto %d | %s -> %s | %s",
                id, utilizadorId, postoId, dataInicio.format(formato), dataFim.format(formato), estado);

        // Se tiver os campos extra, mostra-os tambem
        if (codigoPosto != null) {
            texto += String.format(" | %s | custo estimado: %.2f EUR", codigoPosto, custoEstimado);
        }
        return texto;
    }
}
