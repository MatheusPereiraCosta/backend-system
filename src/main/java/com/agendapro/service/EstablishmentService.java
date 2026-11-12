package com.agendapro.service;

import com.agendapro.domain.Establishment;
import com.agendapro.domain.User;
import com.agendapro.dto.AuthResponse;
import com.agendapro.dto.CreateEstablishmentRequest;
import com.agendapro.dto.EstablishmentDTO;
import com.agendapro.dto.EstablishmentSummaryDTO;
import com.agendapro.exception.DuplicateResourceException;
import com.agendapro.exception.ResourceNotFoundException;
import com.agendapro.repository.EstablishmentRepository;
import com.agendapro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EstablishmentService {

    private static final int MAX_SLUG_BASE_LENGTH = 80;

    private final EstablishmentRepository establishmentRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    /**
     * Cadastra o estabelecimento do dono logado. Cada conta pode ter apenas um.
     * Devolve um novo token, já com o claim establishmentId, para o painel passar a funcionar.
     */
    @Transactional
    public AuthResponse create(UUID userId, CreateEstablishmentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        if (user.getEstablishmentId() != null) {
            throw new DuplicateResourceException("Esta conta já possui um estabelecimento cadastrado.");
        }

        Establishment saved = establishmentRepository.save(Establishment.builder()
                .name(request.getName().trim())
                .slug(generateUniqueSlug(request.getName()))
                .category(request.getCategory().trim())
                .whatsapp(request.getWhatsapp().trim())
                .phone(request.getPhone())
                .tagline(request.getTagline())
                .description(request.getDescription())
                .street(request.getStreet())
                .number(request.getNumber())
                .neighborhood(request.getNeighborhood())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .logoUrl(request.getLogoUrl())
                .bannerUrl(request.getBannerUrl())
                .build());

        user.setEstablishmentId(saved.getId());
        return authService.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Page<EstablishmentSummaryDTO> search(String q, String city, String category, Pageable pageable) {
        return establishmentRepository.search(blankIfNull(q), blankIfNull(city), blankIfNull(category), pageable)
                .map(EstablishmentSummaryDTO::fromEntity);
    }

    /** "Studio Café & Cia" -> "studio-cafe-cia". */
    public static String slugify(String text) {
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slug.length() > MAX_SLUG_BASE_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_BASE_LENGTH).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "estabelecimento" : slug;
    }

    private String generateUniqueSlug(String name) {
        String base = slugify(name);
        String candidate = base;
        int suffix = 2;
        while (establishmentRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String blankIfNull(String value) {
        return value == null ? "" : value.trim();
    }

    @Transactional(readOnly = true)
    public EstablishmentDTO findById(UUID id) {
        return establishmentRepository.findById(id)
                .map(EstablishmentDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento não encontrado com ID: " + id));
    }

    @Transactional(readOnly = true)
    public EstablishmentDTO findBySlug(String slug) {
        return establishmentRepository.findBySlug(slug)
                .map(EstablishmentDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento não encontrado com slug: " + slug));
    }

    @Transactional(readOnly = true)
    public Establishment getEntityById(UUID id) {
        return establishmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento não encontrado com ID: " + id));
    }

    @Transactional
    public EstablishmentDTO update(UUID id, EstablishmentDTO dto) {
        Establishment entity = getEntityById(id);
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setTagline(dto.getTagline());
        entity.setDescription(dto.getDescription());
        entity.setPhone(dto.getPhone());
        entity.setWhatsapp(dto.getWhatsapp());
        entity.setStreet(dto.getStreet());
        entity.setNumber(dto.getNumber());
        entity.setNeighborhood(dto.getNeighborhood());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZipCode(dto.getZipCode());
        entity.setLogoUrl(dto.getLogoUrl());
        entity.setBannerUrl(dto.getBannerUrl());
        return EstablishmentDTO.fromEntity(establishmentRepository.save(entity));
    }
}
