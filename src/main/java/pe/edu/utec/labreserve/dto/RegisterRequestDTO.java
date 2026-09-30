package pe.edu.utec.labreserve.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(

        @NotBlank(message = "username es obligatorio")
        @Size(max = 60, message = "username no puede superar 60 caracteres")
        String username,

        @NotBlank(message = "email es obligatorio")
        @Email(message = "email debe tener un formato valido")
        @Size(max = 120, message = "email no puede superar 120 caracteres")
        String email,

        @NotBlank(message = "password es obligatorio")
        @Size(min = 8, message = "password debe tener al menos 8 caracteres")
        String password
) {
}
