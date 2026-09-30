package pe.edu.utec.labreserve.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponseDTO<T>(List<T> content, int page, int size, long totalElements) {

    public static <E, T> PageResponseDTO<T> from(Page<E> source, Function<E, T> mapper) {
        return new PageResponseDTO<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements()
        );
    }
}
