package ec.intesud.catalogo;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CatalogoService {

    private static final Pattern FORMATO_CODIGO = Pattern.compile("[A-Z]{3}-\\d{3}");
    private final Map<String, Curso> cursos = new LinkedHashMap<>();

    public Curso registrar(String codigo, String nombre, int cupo, BigDecimal precio) {
        if (codigo == null || !FORMATO_CODIGO.matcher(codigo).matches()) {
            throw new IllegalArgumentException("Código inválido: " + codigo);
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (cupo < 5 || cupo > 40) {
            throw new IllegalArgumentException("Cupo fuera de rango: " + cupo);
        }
        if (precio == null || precio.signum() <= 0) {
            throw new IllegalArgumentException("Precio inválido: " + precio);
        }
        if (cursos.containsKey(codigo)) {
            throw new IllegalStateException("Ya existe el curso " + codigo);
        }
        Curso curso = new Curso(codigo, nombre.trim(), cupo, precio);
        cursos.put(codigo, curso);
        return curso;
    }

    public List<Curso> buscar(String texto) {
        return cursos.values().stream()
                .filter(c -> c.getNombre().contains(texto))
                .toList();
    }

    public void inscribir(String codigo) {
        Curso curso = obtener(codigo);
        if (curso.getInscritos() > curso.getCupo()) {
            throw new IllegalStateException("No hay cupos disponibles");
        }
        curso.inscribir();
    }

    public BigDecimal precioGrupal(String codigo, int estudiantes) {
        if (estudiantes <= 0) {
            throw new IllegalArgumentException("Cantidad inválida: " + estudiantes);
        }
        Curso curso = obtener(codigo);
        BigDecimal total = curso.getPrecio().multiply(BigDecimal.valueOf(estudiantes));
        if (estudiantes > 3) {
            total = total.multiply(new BigDecimal("0.85"));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public Curso obtener(String codigo) {
        Curso curso = cursos.get(codigo);
        if (curso == null) {
            throw new NoSuchElementException("No existe el curso " + codigo);
        }
        return curso;
    }
}
