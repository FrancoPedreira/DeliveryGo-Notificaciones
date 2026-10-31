package notificaciones.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

// DTO-10: salida paginada de API-04 (CU-06), con los parámetros de paginación de la consigna (page, size, sort)
@Schema(description = "DTO-10: página de resultados")
public class PaginaResponseDto<T> {
    private List<T> content;
    @Schema(description = "Número de página, desde 0", example = "0")
    private int page;
    @Schema(example = "20")
    private int size;
    @Schema(description = "Total de elementos que cumplen el filtro", example = "42")
    private long totalElements;
    @Schema(example = "3")
    private int totalPages;

    public PaginaResponseDto(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    // Getters: solo lectura, Jackson los usa para armar el JSON de respuesta
    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
