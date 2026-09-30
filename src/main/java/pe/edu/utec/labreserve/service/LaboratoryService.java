package pe.edu.utec.labreserve.service;

import pe.edu.utec.labreserve.dto.CreateLaboratoryRequestDTO;
import pe.edu.utec.labreserve.dto.LaboratoryResponseDTO;
import pe.edu.utec.labreserve.entity.Laboratory;
import pe.edu.utec.labreserve.entity.LaboratoryStatus;
import pe.edu.utec.labreserve.entity.User;
import pe.edu.utec.labreserve.exception.BusinessRuleException;
import pe.edu.utec.labreserve.exception.ResourceNotFoundException;
import pe.edu.utec.labreserve.repository.LaboratoryRepository;
import pe.edu.utec.labreserve.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LaboratoryService {

    private final LaboratoryRepository laboratoryRepository;
    private final UserRepository userRepository;

    public LaboratoryService(LaboratoryRepository laboratoryRepository, UserRepository userRepository) {
        this.laboratoryRepository = laboratoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LaboratoryResponseDTO create(CreateLaboratoryRequestDTO request) {
        if (laboratoryRepository.existsByName(request.name())) {
            throw new BusinessRuleException("Ya existe un laboratorio con ese nombre");
        }

        User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", request.managerId()));

        Laboratory laboratory = new Laboratory(
                request.name(), request.location(), manager, LaboratoryStatus.ACTIVE);

        return LaboratoryResponseDTO.from(laboratoryRepository.save(laboratory));
    }

    @Transactional(readOnly = true)
    public List<LaboratoryResponseDTO> findAll() {
        return laboratoryRepository.findAll().stream()
                .map(LaboratoryResponseDTO::from)
                .toList();
    }
}
