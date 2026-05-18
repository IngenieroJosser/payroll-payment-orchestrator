package com.corvian.payroll_payment_orchestrator.iam.application;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.iam.domain.ApiClientStatus;
import com.corvian.payroll_payment_orchestrator.iam.domain.UserStatus;
import com.corvian.payroll_payment_orchestrator.iam.infrastructure.*;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class IamService {
    private final JpaUserRepository userRepository;
    private final JpaRoleRepository roleRepository;
    private final JpaApiClientRepository apiClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final SecureRandom random = new SecureRandom();

    public IamService(JpaUserRepository userRepository, JpaRoleRepository roleRepository, JpaApiClientRepository apiClientRepository, PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.apiClientRepository = apiClientRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UserEntity createUser(UUID tenantId, UUID companyId, String email, String fullName, String password, List<String> roleNames) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DomainException("USER_ALREADY_EXISTS", "User email is already registered");
        }
        Set<RoleEntity> roles = new HashSet<>();
        for (String roleName : roleNames == null || roleNames.isEmpty() ? List.of("PAYROLL_OPERATOR") : roleNames) {
            roles.add(roleRepository.findByName(roleName).orElseThrow(() -> new DomainException("ROLE_NOT_FOUND", "Role not found: " + roleName)));
        }
        OffsetDateTime now = OffsetDateTime.now();
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setTenantId(tenantId);
        user.setCompanyId(companyId);
        user.setEmail(email.trim().toLowerCase());
        user.setFullName(fullName.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(roles);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        UserEntity saved = userRepository.save(user);
        auditLogService.record("USER_CREATED", "USER", saved.getId(), "User created: " + saved.getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<UserEntity> findAllUsers() { return userRepository.findAll(); }

    @Transactional
    public CreateApiClientResult createApiClient(UUID companyId, String name, List<String> scopes) {
        String clientId = "cli_" + token(18);
        String secret = "sec_" + token(40);
        ApiClientEntity entity = new ApiClientEntity();
        entity.setId(UUID.randomUUID());
        entity.setCompanyId(companyId);
        entity.setClientId(clientId);
        entity.setClientSecretHash(passwordEncoder.encode(secret));
        entity.setName(name.trim());
        entity.setScopes(String.join(" ", scopes == null || scopes.isEmpty() ? List.of("payroll:create", "payroll:read") : scopes));
        entity.setStatus(ApiClientStatus.ACTIVE);
        entity.setCreatedAt(OffsetDateTime.now());
        ApiClientEntity saved = apiClientRepository.save(entity);
        auditLogService.record("API_CLIENT_CREATED", "API_CLIENT", saved.getId(), "API client created: " + saved.getName());
        return new CreateApiClientResult(saved, secret);
    }

    private String token(int bytes) {
        byte[] buffer = new byte[bytes];
        random.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    public record CreateApiClientResult(ApiClientEntity entity, String plainSecret) {}
}
