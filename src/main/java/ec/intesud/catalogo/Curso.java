package ec.intesud.catalogo;

import java.math.BigDecimal;

public class Curso {
    private final String codigo;
    private final String nombre;
    private final int cupo;
    private final BigDecimal precio;
    private int inscritos;

    public Curso(String codigo, String nombre, int cupo, BigDecimal precio) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.cupo = cupo;
        this.precio = precio;
    }

    public void inscribir() { inscritos++; }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public int getCupo() { return cupo; }
    public BigDecimal getPrecio() { return precio; }
    public int getInscritos() { return inscritos; }
}
