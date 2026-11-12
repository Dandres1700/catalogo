// Paquete de la prueba (el mismo de la clase que probamos)
package ec.intesud.catalogo;

// Métodos para verificar resultados
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow; // verifica que NO se lance una excepción
import static org.junit.jupiter.api.Assertions.assertEquals; // compara esperado y real
import static org.junit.jupiter.api.Assertions.assertThrows; // verifica que se lance una excepción

// Utilidades
import java.math.BigDecimal; // precios exactos
import java.util.List; // resultado de la búsqueda
import java.util.NoSuchElementException; // error "no existe"
// Anotaciones de JUnit 5
import org.junit.jupiter.api.BeforeEach; // se ejecuta antes de cada prueba
import org.junit.jupiter.api.DisplayName; // nombre legible de la prueba
import org.junit.jupiter.api.Test; // prueba simple
import org.junit.jupiter.params.ParameterizedTest; // prueba que se repite con varios datos
import org.junit.jupiter.params.provider.CsvSource; // datos de la prueba, separados por comas

// Pruebas de las reglas R1 a R8 del catálogo de cursos
class CatalogoServiceTest {

    // Precio válido que usamos cuando el precio no es lo que se está probando
    private static final BigDecimal PRECIO = new BigDecimal("100.00");

    // Servicio que vamos a probar
    private CatalogoService service;

    // Antes de cada prueba creamos un catálogo vacío: así una prueba no afecta a otra
    @BeforeEach
    void crearServicio() {
        service = new CatalogoService();
    }

    // ---------- R1: formato del código ----------

    @Test
    @DisplayName("R1: un código con tres mayúsculas, guion y tres dígitos se registra")
    void codigoValidoSeRegistra() {
        // ACT: registramos un curso con el código del ejemplo de la regla
        Curso curso = service.registrar("JAV-101", "Java básico", 20, PRECIO);
        // ASSERT: el curso guardó el mismo código
        assertEquals("JAV-101", curso.getCodigo());
    }

    // Se ejecuta una vez por cada código: minúsculas, sin guion, letras o dígitos de más o de menos
    @ParameterizedTest
    @CsvSource({"jav-101", "JAV101", "JA-101", "JAVA-101", "JAV-10", "JAV-1010", "J4V-101"})
    @DisplayName("R1: un código con formato incorrecto se rechaza")
    void codigoConFormatoIncorrectoLanzaExcepcion(String codigo) {
        // ACT + ASSERT: registrar debe lanzar IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(codigo, "Java básico", 20, PRECIO));
    }

    @Test
    @DisplayName("R1: un código nulo se rechaza")
    void codigoNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(null, "Java básico", 20, PRECIO));
    }

    // ---------- R2: nombre obligatorio ----------

    @Test
    @DisplayName("R2: un nombre vacío se rechaza")
    void nombreVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("JAV-101", "", 20, PRECIO));
    }

    @Test
    @DisplayName("R2: un nombre con solo espacios se rechaza")
    void nombreSoloEspaciosLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("JAV-101", "   ", 20, PRECIO));
    }

    @Test
    @DisplayName("R2: un nombre nulo se rechaza")
    void nombreNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("JAV-101", null, 20, PRECIO));
    }

    // ---------- R3: cupo entre 5 y 40, ambos incluidos ----------

    // Valores límite válidos: 5 y 40, más uno intermedio
    @ParameterizedTest
    @CsvSource({"5", "20", "40"})
    @DisplayName("R3: un cupo entre 5 y 40 (incluidos) se acepta")
    void cupoDentroDelRangoSeAcepta(int cupo) {
        // ACT + ASSERT: registrar no debe lanzar ninguna excepción
        Curso curso = assertDoesNotThrow(
                () -> service.registrar("JAV-101", "Java básico", cupo, PRECIO));
        // ASSERT: el curso guardó el cupo pedido
        assertEquals(cupo, curso.getCupo());
    }

    // Valores límite inválidos: 4 (uno menos del mínimo) y 41 (uno más del máximo)
    @ParameterizedTest
    @CsvSource({"4", "41"})
    @DisplayName("R3: un cupo menor que 5 o mayor que 40 se rechaza")
    void cupoFueraDelRangoLanzaExcepcion(int cupo) {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("JAV-101", "Java básico", cupo, PRECIO));
    }

    // ---------- R4: precio mayor que cero ----------

    @Test
    @DisplayName("R4: un precio igual a cero se rechaza")
    void precioCeroLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("JAV-101", "Java básico", 20, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("R4: un precio negativo se rechaza")
    void precioNegativoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("JAV-101", "Java básico", 20, new BigDecimal("-1.00")));
    }

    @Test
    @DisplayName("R4: un precio mayor que cero se acepta")
    void precioPositivoSeAcepta() {
        // ACT: el precio más pequeño posible con dos decimales
        Curso curso = service.registrar("JAV-101", "Java básico", 20, new BigDecimal("0.01"));
        // ASSERT: el curso guardó ese precio
        assertEquals(new BigDecimal("0.01"), curso.getPrecio());
    }

    // ---------- R5: códigos únicos ----------

    @Test
    @DisplayName("R5: no se puede registrar dos cursos con el mismo código")
    void codigoDuplicadoLanzaExcepcion() {
        // ARRANGE: ya existe un curso con el código JAV-101
        service.registrar("JAV-101", "Java básico", 20, PRECIO);
        // ACT + ASSERT: registrar otro con el mismo código debe fallar
        assertThrows(IllegalStateException.class,
                () -> service.registrar("JAV-101", "Java avanzado", 30, PRECIO));
    }

    // ---------- R6: búsqueda sin distinguir mayúsculas ----------

    // El mismo texto escrito de cuatro formas debe encontrar el mismo curso
    @ParameterizedTest
    @CsvSource({"Java", "java", "JAVA", "jAvA"})
    @DisplayName("R6: la búsqueda por nombre no distingue mayúsculas de minúsculas")
    void busquedaNoDistingueMayusculas(String texto) {
        // ARRANGE: dos cursos, solo uno contiene "Java" en el nombre
        service.registrar("JAV-101", "Java básico", 20, PRECIO);
        service.registrar("PYT-101", "Python básico", 20, PRECIO);
        // ACT: buscamos el texto
        List<Curso> encontrados = service.buscar(texto);
        // ASSERT: aparece un solo curso y es el de Java
        assertEquals(1, encontrados.size());
        assertEquals("JAV-101", encontrados.get(0).getCodigo());
    }

    // ---------- R7: inscritos no superan el cupo ----------

    @Test
    @DisplayName("R7: se puede inscribir estudiantes hasta completar el cupo")
    void sePuedeInscribirHastaCompletarElCupo() {
        // ARRANGE: curso con cupo 5
        service.registrar("JAV-101", "Java básico", 5, PRECIO);
        // ACT: inscribimos exactamente 5 estudiantes
        for (int i = 0; i < 5; i++) {
            service.inscribir("JAV-101");
        }
        // ASSERT: el curso tiene 5 inscritos
        assertEquals(5, service.obtener("JAV-101").getInscritos());
    }

    @Test
    @DisplayName("R7: con el cupo lleno, una inscripción más se rechaza")
    void inscribirConCupoLlenoLanzaExcepcion() {
        // ARRANGE: curso con cupo 5 y sus 5 cupos ocupados
        service.registrar("JAV-101", "Java básico", 5, PRECIO);
        for (int i = 0; i < 5; i++) {
            service.inscribir("JAV-101");
        }
        // ACT + ASSERT: el sexto estudiante no debe poder inscribirse
        assertThrows(IllegalStateException.class, () -> service.inscribir("JAV-101"));
        // ASSERT: los inscritos siguen siendo 5, no más que el cupo
        assertEquals(5, service.obtener("JAV-101").getInscritos());
    }

    @Test
    @DisplayName("R7: inscribir en un curso que no existe lanza NoSuchElementException")
    void inscribirEnCursoInexistenteLanzaExcepcion() {
        assertThrows(NoSuchElementException.class, () -> service.inscribir("XXX-999"));
    }

    // ---------- R8: descuento grupal del 15 % desde 3 estudiantes ----------

    // Valores límite: 2 (sin descuento) y 3 (con descuento). Precio del curso: 100.00
    @ParameterizedTest
    @CsvSource({
            "1, 100.00", // 1 x 100 sin descuento
            "2, 200.00", // 2 x 100 sin descuento (límite inferior)
            "3, 255.00", // 3 x 100 = 300 menos 15 % (límite donde empieza el descuento)
            "4, 340.00"  // 4 x 100 = 400 menos 15 %
    })
    @DisplayName("R8: el descuento del 15 % se aplica a grupos de 3 o más estudiantes")
    void precioGrupalAplicaDescuentoDesdeTresEstudiantes(int estudiantes, BigDecimal esperado) {
        // ARRANGE: curso de 100.00 por estudiante
        service.registrar("JAV-101", "Java básico", 20, PRECIO);
        // ACT: calculamos el precio del grupo
        BigDecimal total = service.precioGrupal("JAV-101", estudiantes);
        // ASSERT: comparamos con el total esperado
        assertEquals(esperado, total);
    }

    @Test
    @DisplayName("R8: una cantidad de estudiantes igual a cero se rechaza")
    void precioGrupalConCeroEstudiantesLanzaExcepcion() {
        // ARRANGE: el curso existe
        service.registrar("JAV-101", "Java básico", 20, PRECIO);
        // ACT + ASSERT: un grupo de 0 estudiantes no tiene sentido
        assertThrows(IllegalArgumentException.class, () -> service.precioGrupal("JAV-101", 0));
    }
}
