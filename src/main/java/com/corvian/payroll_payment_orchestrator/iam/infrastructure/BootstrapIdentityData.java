package com.corvian.payroll_payment_orchestrator.iam.infrastructure;

import com.corvian.payroll_payment_orchestrator.iam.domain.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Component
public class BootstrapIdentityData implements CommandLineRunner {
    private final JpaPermissionRepository permissionRepository;
    private final JpaRoleRepository roleRepository;
    private final JpaUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public BootstrapIdentityData(
            JpaPermissionRepository permissionRepository,
            JpaRoleRepository roleRepository,
            JpaUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.email:admin@corvian.local}") String adminEmail,
            @Value("${app.bootstrap.admin.password:Admin123!}") String adminPassword
    ) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, String> permissions = new LinkedHashMap<>();
        permissions.put("iam:manage", "Manage users, roles and API clients");
        permissions.put("iam:read", "Read identity resources");
        permissions.put("tenant:manage", "Manage tenants");
        permissions.put("company:manage", "Manage companies and bank accounts");
        permissions.put("payroll:create", "Create payroll batches");
        permissions.put("payroll:read", "Read payroll batches");
        permissions.put("payroll:approve", "Approve or reject payroll batches");
        permissions.put("payroll:execute", "Execute payroll batches");
        permissions.put("audit:read", "Read audit logs");
        permissions.put("webhook:manage", "Manage webhooks");
        permissions.put("bank:manage", "Manage bank integrations");
        permissions.put("reconciliation:manage", "Manage bank reconciliation");

        Map<String, PermissionEntity> created = new HashMap<>();
        for (var entry : permissions.entrySet()) {
            PermissionEntity permission = permissionRepository.findByName(entry.getKey()).orElseGet(() -> {
                PermissionEntity p = new PermissionEntity();
                p.setId(UUID.randomUUID());
                p.setName(entry.getKey());
                p.setDescription(entry.getValue());
                return permissionRepository.save(p);
            });
            created.put(permission.getName(), permission);
        }

        upsertRole("ADMIN", "Platform administrator", created.values());
        upsertRole("AUDITOR", "Read-only audit operator", List.of(created.get("audit:read"), created.get("payroll:read")));
        upsertRole("PAYROLL_OPERATOR", "Payroll operator", List.of(created.get("payroll:create"), created.get("payroll:read")));
        upsertRole("APPROVER", "Payroll approver", List.of(created.get("payroll:read"), created.get("payroll:approve")));
        upsertRole("BANK_OPERATOR", "Bank integration operator", List.of(created.get("bank:manage"), created.get("reconciliation:manage"), created.get("payroll:read")));

        if (!userRepository.existsByEmailIgnoreCase(adminEmail)) {
            UserEntity admin = new UserEntity();
            admin.setId(UUID.randomUUID());
            admin.setEmail(adminEmail.toLowerCase());
            admin.setFullName("Default Administrator");
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setStatus(UserStatus.ACTIVE);
            admin.setRoles(Set.of(roleRepository.findByName("ADMIN").orElseThrow()));
            admin.setCreatedAt(OffsetDateTime.now());
            admin.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(admin);
        }
    }

    private void upsertRole(String name, String description, Collection<PermissionEntity> permissions) {
        RoleEntity role = roleRepository.findByName(name).orElseGet(() -> {
            RoleEntity r = new RoleEntity();
            r.setId(UUID.randomUUID());
            r.setName(name);
            r.setDescription(description);
            return r;
        });
        role.setPermissions(new HashSet<>(permissions));
        roleRepository.save(role);
    }
}
