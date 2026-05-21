package com.chatsever.role.service;

import com.chatsever.role.model.Role;
import com.chatsever.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository repository;

    public Role createRole(String serverId, String roleName, String color, String permissions) {
        Role role = Role.builder()
                .serverId(serverId)
                .roleName(roleName)
                .color(color)
                .permissions(permissions != null ? permissions : "SEND_MESSAGE")
                .build();
        return repository.save(role);
    }

    public List<Role> getRolesByServer(String serverId) {
        return repository.findByServerId(serverId);
    }

    public String deleteRole(String roleId) {
        repository.deleteById(roleId);
        return "Xóa vai trò thành công!";
    }
}