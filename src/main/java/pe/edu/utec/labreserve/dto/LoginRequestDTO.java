package pe.edu.utec.labreserve.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(

        @NotBlank(message = "username es obligatorio")
        String username,

        @NotBlank(message = "password es obligatorio")
        String password
) {
}
