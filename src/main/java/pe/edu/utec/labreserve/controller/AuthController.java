package pe.edu.utec.labreserve.controller;

import jakarta.validation.Valid;
import pe.edu.utec.labreserve.dto.LoginRequestDTO;
import pe.edu.utec.labreserve.dto.LoginResponseDTO;
import pe.edu.utec.labreserve.dto.RegisterRequestDTO;
import pe.edu.utec.labreserve.dto.RegisterResponseDTO;
import pe.edu.utec.labreserve.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
