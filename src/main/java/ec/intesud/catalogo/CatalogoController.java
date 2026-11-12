package ec.intesud.catalogo;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/cursos")
public class CatalogoController {

    private final CatalogoService service;

    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    public record CursoRequest(String codigo, String nombre,
                               int cupo, BigDecimal precio) { }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Curso registrar(@RequestBody CursoRequest r) {
        return service.registrar(r.codigo(), r.nombre(), r.cupo(), r.precio());
    }

    @GetMapping
    public List<Curso> buscar(@RequestParam(defaultValue = "") String texto) {
        return service.buscar(texto);
    }

    @PostMapping("/{codigo}/inscripciones")
    public Curso inscribir(@PathVariable String codigo) {
        service.inscribir(codigo);
        return service.obtener(codigo);
    }

    @GetMapping("/{codigo}/precio-grupal")
    public BigDecimal precioGrupal(@PathVariable String codigo,
                                   @RequestParam int estudiantes) {
        return service.precioGrupal(codigo, estudiantes);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> datosInvalidos(RuntimeException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> noEncontrado(RuntimeException e) {
        return Map.of("error", e.getMessage());
    }
}
