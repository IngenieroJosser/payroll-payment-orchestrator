package com.corvian.payroll_payment_orchestrator.iam.application;

import com.corvian.payroll_payment_orchestrator.iam.infrastructure.RoleEntity;
import com.corvian.payroll_payment_orchestrator.iam.infrastructure.UserEntity;
import java.util.List;

public class AuthorityService {
    private AuthorityService() {}

    public static List<String> authorities(UserEntity user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream().map(permission -> permission.getName()))
                .distinct()
                .sorted()
                .toList();
    }

    public static List<String> roleNames(UserEntity user) {
        return user.getRoles().stream().map(RoleEntity::getName).sorted().toList();
    }
}
