package pe.edu.utec.labreserve.service;

import pe.edu.utec.labreserve.dto.LoginRequestDTO;
import pe.edu.utec.labreserve.dto.LoginResponseDTO;
import pe.edu.utec.labreserve.dto.RegisterRequestDTO;
import pe.edu.utec.labreserve.dto.RegisterResponseDTO;
import pe.edu.utec.labreserve.entity.Role;
import pe.edu.utec.labreserve.entity.User;
import pe.edu.utec.labreserve.exception.BusinessRuleException;
import pe.edu.utec.labreserve.exception.InvalidCredentialsException;
import pe.edu.utec.labreserve.repository.UserRepository;
import pe.edu.utec.labreserve.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessRuleException("El username ya esta registrado");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("El email ya esta registrado");
        }

        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.ROLE_STUDENT
        );
        User saved = userRepository.save(user);
        return new RegisterResponseDTO(saved.getId(), saved.getUsername(), saved.getEmail());
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException("Credenciales invalidas");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales invalidas"));

        return new LoginResponseDTO(jwtService.generateToken(user), jwtService.getExpirationSeconds());
    }
}
