package common;

import java.io.Serializable;

// Um condutor registado no sistema (tabela utilizador).
public class Utilizador implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String nome;
    private String email;
    private String matricula;
    private TipoFicha tipoFicha;

    // utilizador novo (o id e gerado pela BD)
    public Utilizador(String nome, String email, String matricula, TipoFicha tipoFicha) {
        this(0, nome, email, matricula, tipoFicha);
    }

    public Utilizador(int id, String nome, String email, String matricula, TipoFicha tipoFicha) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.matricula = matricula;
        this.tipoFicha = tipoFicha;
    }

    public int getId()              { return id; }
    public String getNome()         { return nome; }
    public String getEmail()        { return email; }
    public String getMatricula()    { return matricula; }
    public TipoFicha getTipoFicha() { return tipoFicha; }

    @Override
    public String toString() {
        return String.format("#%d %s (%s) - %s, ficha %s",
                id, nome, email, matricula, tipoFicha);
    }
}
