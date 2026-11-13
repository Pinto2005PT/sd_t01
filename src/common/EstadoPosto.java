package common;

// Estado operacional do posto + maquina de estados.
public enum EstadoPosto {
    DISPONIVEL,
    RESERVADO,
    EM_MANUTENCAO,
    DESATIVADO;

    // O admin pode mudar deste estado (this) para o estado 'novo'?
    public boolean adminPodeMudarPara(EstadoPosto novo) {
        if (this == DISPONIVEL)    return novo == EM_MANUTENCAO || novo == DESATIVADO;
        if (this == RESERVADO)     return novo == EM_MANUTENCAO;
        if (this == EM_MANUTENCAO) return novo == DISPONIVEL || novo == DESATIVADO;
        if (this == DESATIVADO)    return novo == DISPONIVEL;
        return false;
    }

    // O sistema (reservas) pode mudar deste estado (this) para o estado 'novo'?
    public boolean sistemaPodeMudarPara(EstadoPosto novo) {
        if (this == DISPONIVEL && novo == RESERVADO) return true;
        if (this == RESERVADO  && novo == DISPONIVEL) return true;
        return false;
    }

    // Um posto neste estado aceita novas reservas?
    public boolean aceitaReservas() {
        return this == DISPONIVEL || this == RESERVADO;
    }
}
