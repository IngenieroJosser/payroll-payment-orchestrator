package com.corvian.payroll_payment_orchestrator.iam.application;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.iam.domain.ApiClientStatus;
import com.corvian.payroll_payment_orchestrator.iam.domain.UserStatus;
import com.corvian.payroll_payment_orchestrator.iam.infrastructure.*;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorType;
import com.corvian.payroll_payment_orchestrator.shared.security.context.AuthenticatedActor;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ResourceAccessService;
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
    private final ResourceAccessService accessService;
    private final SecureRandom random = new SecureRandom();

    public IamService(
            JpaUserRepository userRepository,
            JpaRoleRepository roleRepository,
            JpaApiClientRepository apiClientRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            ResourceAccessService accessService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.apiClientRepository = apiClientRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.accessService = accessService;
    }

    @Transactional
    public UserEntity createUser(UUID tenantId, UUID companyId, String email, String fullName, String password, List<String> roleNames) {
        if (tenantId == null) throw new DomainException("TENANT_REQUIRED", "Tenant is required for non-platform users");
        accessService.requireTenantAccess(tenantId);
        if (companyId != null) {
            var company = accessService.requireCompanyAccess(companyId);
            if (!tenantId.equals(company.getTenantId())) {
                throw new DomainException("TENANT_COMPANY_MISMATCH", "Company does not belong to the requested tenant");
            }
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DomainException("USER_ALREADY_EXISTS", "User email is already registered");
        }
        if (password == null || password.length() < 12) {
            throw new DomainException("WEAK_PASSWORD", "Password must contain at least 12 characters");
        }
        Set<RoleEntity> roles = new HashSet<>();
        for (String roleName : roleNames == null || roleNames.isEmpty() ? List.of("PAYROLL_OPERATOR") : roleNames) {
            if ("ADMIN".equals(roleName) && !accessService.currentActor().platformAdmin()) {
                throw new DomainException("ROLE_ASSIGNMENT_DENIED", "Only a platform administrator may assign the ADMIN role");
            }
            roles.add(roleRepository.findByName(roleName)
                    .orElseThrow(() -> new DomainException("ROLE_NOT_FOUND", "Role not found: " + roleName)));
        }
        OffsetDateTime now = OffsetDateTime.now();
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setTenantId(tenantId);
        user.setCompanyId(companyId);
        user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        user.setFullName(fullName.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(roles);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        UserEntity saved = userRepository.save(user);
        auditLogService.record("USER_CREATED", "USER", saved.getId(), "User created", tenantId, companyId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<UserEntity> findAllUsers() {
        AuthenticatedActor actor = accessService.currentActor();
        if (actor.platformAdmin() || actor.actorType() == ActorType.SYSTEM) return userRepository.findAll();
        if (actor.companyId() != null) return userRepository.findByCompanyId(actor.companyId());
        if (actor.tenantId() != null) return userRepository.findByTenantId(actor.tenantId());
        return List.of();
    }

    @Transactional
    public CreateApiClientResult createApiClient(UUID companyId, String name, List<String> scopes) {
        if (companyId == null) throw new DomainException("COMPANY_REQUIRED", "Company is required for an API client");
        var company = accessService.requireCompanyAccess(companyId);
        List<String> normalizedScopes = normalizeScopes(scopes);
        String clientId = "cli_" + token(18);
        String secret = "sec_" + token(40);
        ApiClientEntity entity = new ApiClientEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(company.getTenantId());
        entity.setCompanyId(companyId);
        entity.setClientId(clientId);
        entity.setClientSecretHash(passwordEncoder.encode(secret));
        entity.setName(name.trim());
        entity.setScopes(String.join(" ", normalizedScopes));
        entity.setStatus(ApiClientStatus.ACTIVE);
        entity.setCreatedAt(OffsetDateTime.now());
        ApiClientEntity saved = apiClientRepository.save(entity);
        auditLogService.record("API_CLIENT_CREATED", "API_CLIENT", saved.getId(), "API client created", company.getTenantId(), companyId);
        return new CreateApiClientResult(saved, secret);
    }

    private List<String> normalizeScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return List.of("payroll:create", "payroll:read");
        }
        TreeSet<String> normalized = new TreeSet<>();
        for (String scope : scopes) {
            if (scope != null) {
                String value = scope.trim();
                if (!value.isBlank()) normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }

    private String token(int bytes) {
        byte[] buffer = new byte[bytes];
        random.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    public record CreateApiClientResult(ApiClientEntity entity, String plainSecret) {}
}
