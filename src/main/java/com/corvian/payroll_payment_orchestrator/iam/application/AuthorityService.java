package com.corvian.payroll_payment_orchestrator.iam.application;

import com.corvian.payroll_payment_orchestrator.iam.infrastructure.PermissionEntity;
import com.corvian.payroll_payment_orchestrator.iam.infrastructure.RoleEntity;
import com.corvian.payroll_payment_orchestrator.iam.infrastructure.UserEntity;

import java.util.List;
import java.util.TreeSet;

public final class AuthorityService {
    private AuthorityService() {}

    public static List<String> authorities(UserEntity user) {
        TreeSet<String> authorities = new TreeSet<>();
        for (RoleEntity role : user.getRoles()) {
            authorities.add("ROLE_" + role.getName());
            for (PermissionEntity permission : role.getPermissions()) {
                authorities.add(permission.getName());
            }
        }
        return List.copyOf(authorities);
    }

    public static List<String> roleNames(UserEntity user) {
        TreeSet<String> roleNames = new TreeSet<>();
        for (RoleEntity role : user.getRoles()) {
            roleNames.add(role.getName());
        }
        return List.copyOf(roleNames);
    }
}
