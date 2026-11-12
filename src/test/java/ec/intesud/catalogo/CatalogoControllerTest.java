// Paquete de la prueba (el mismo del controlador)
package ec.intesud.catalogo;

// Métodos de Mockito
import static org.mockito.Mockito.doThrow; // hace que un método void del mock lance una excepción
import static org.mockito.Mockito.when; // define qué devuelve el mock
// Métodos de MockMvc para simular peticiones HTTP y revisar la respuesta
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // petición GET
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // petición POST
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath; // revisa el JSON
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status; // revisa el código HTTP

// Utilidades y anotaciones
import java.math.BigDecimal; // precios exactos
import java.util.List; // resultado de la búsqueda
import java.util.NoSuchElementException; // error "no existe"
import org.junit.jupiter.api.DisplayName; // nombre legible de la prueba
import org.junit.jupiter.api.Test; // prueba simple
import org.springframework.beans.factory.annotation.Autowired; // Spring entrega el objeto
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; // prueba solo la capa web
import org.springframework.http.MediaType; // tipo de contenido JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean; // mock dentro de Spring
import org.springframework.test.web.servlet.MockMvc; // simula peticiones HTTP

// @WebMvcTest levanta solo el controlador (no el servicio real)
@WebMvcTest(CatalogoController.class)
class CatalogoControllerTest {

    // Herramienta para hacer peticiones HTTP falsas
    @Autowired
    MockMvc mvc;

    // El servicio real se reemplaza por un mock: aquí solo probamos el controlador
    @MockitoBean
    CatalogoService service;

    @Test
    @DisplayName("POST /api/cursos con datos válidos devuelve 201 y el curso creado")
    void registrarCursoValidoDevuelve201() throws Exception {
        // ARRANGE: el mock devuelve el curso registrado
        BigDecimal precio = new BigDecimal("100.00");
        when(service.registrar("JAV-101", "Java", 20, precio))
                .thenReturn(new Curso("JAV-101", "Java", 20, precio));

        // ACT: simulamos POST /api/cursos con el curso en JSON
        mvc.perform(post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "JAV-101", "nombre": "Java", "cupo": 20, "precio": 100.00}
                                """))
                // ASSERT: código HTTP 201
                .andExpect(status().isCreated())
                // ASSERT: el JSON trae los datos del curso
                .andExpect(jsonPath("$.codigo").value("JAV-101"))
                .andExpect(jsonPath("$.cupo").value(20));
    }

    @Test
    @DisplayName("POST /api/cursos con un dato inválido devuelve 400")
    void datoInvalidoDevuelve400() throws Exception {
        // ARRANGE: el mock rechaza el cupo 41 igual que lo haría el servicio real
        when(service.registrar("JAV-101", "Java", 41, new BigDecimal("100.00")))
                .thenThrow(new IllegalArgumentException("Cupo fuera de rango: 41"));

        // ACT: simulamos POST /api/cursos con cupo 41
        mvc.perform(post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "JAV-101", "nombre": "Java", "cupo": 41, "precio": 100.00}
                                """))
                // ASSERT: código HTTP 400
                .andExpect(status().isBadRequest())
                // ASSERT: el JSON trae el mensaje del error
                .andExpect(jsonPath("$.error").value("Cupo fuera de rango: 41"));
    }

    @Test
    @DisplayName("POST /api/cursos con un código duplicado devuelve 400")
    void codigoDuplicadoDevuelve400() throws Exception {
        // ARRANGE: el mock lanza la excepción de "ya existe"
        when(service.registrar("JAV-101", "Java", 20, new BigDecimal("100.00")))
                .thenThrow(new IllegalStateException("Ya existe el curso JAV-101"));

        // ACT + ASSERT: IllegalStateException también se traduce a 400
        mvc.perform(post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "JAV-101", "nombre": "Java", "cupo": 20, "precio": 100.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ya existe el curso JAV-101"));
    }

    @Test
    @DisplayName("GET precio-grupal de un curso inexistente devuelve 404")
    void cursoInexistenteDevuelve404() throws Exception {
        // ARRANGE: el mock lanza la excepción de "no existe"
        when(service.precioGrupal("XXX-999", 3))
                .thenThrow(new NoSuchElementException("No existe el curso XXX-999"));

        // ACT: simulamos GET /api/cursos/XXX-999/precio-grupal?estudiantes=3
        mvc.perform(get("/api/cursos/XXX-999/precio-grupal")
                        .param("estudiantes", "3"))
                // ASSERT: código HTTP 404
                .andExpect(status().isNotFound())
                // ASSERT: el JSON trae el mensaje del error
                .andExpect(jsonPath("$.error").value("No existe el curso XXX-999"));
    }

    @Test
    @DisplayName("POST inscripciones en un curso inexistente devuelve 404")
    void inscribirEnCursoInexistenteDevuelve404() throws Exception {
        // ARRANGE: inscribir es void, por eso se usa doThrow en lugar de when
        doThrow(new NoSuchElementException("No existe el curso XXX-999"))
                .when(service).inscribir("XXX-999");

        // ACT + ASSERT: la inscripción responde 404
        mvc.perform(post("/api/cursos/XXX-999/inscripciones"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET precio-grupal de un curso existente devuelve 200 con el total")
    void precioGrupalDevuelve200() throws Exception {
        // ARRANGE: el mock responde 255.00 para 3 estudiantes
        when(service.precioGrupal("JAV-101", 3)).thenReturn(new BigDecimal("255.00"));

        // ACT + ASSERT: código 200 y el cuerpo es el total
        mvc.perform(get("/api/cursos/JAV-101/precio-grupal")
                        .param("estudiantes", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(255.00));
    }

    @Test
    @DisplayName("GET /api/cursos devuelve 200 con los cursos encontrados")
    void buscarDevuelveLosCursosEncontrados() throws Exception {
        // ARRANGE: el mock encuentra un curso para el texto "java"
        when(service.buscar("java"))
                .thenReturn(List.of(new Curso("JAV-101", "Java", 20, new BigDecimal("100.00"))));

        // ACT + ASSERT: código 200 y una lista con ese curso
        mvc.perform(get("/api/cursos").param("texto", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("JAV-101"));
    }
}
