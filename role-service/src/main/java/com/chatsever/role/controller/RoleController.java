package com.chatsever.role.controller;

import com.chatsever.role.model.Role;
import com.chatsever.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping("/{serverId}/roles")
    public ResponseEntity<Role> createRole(@PathVariable String serverId, @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(roleService.createRole(
                serverId,
                payload.get("roleName"),
                payload.get("color"),
                payload.get("permissions")
        ));
    }

    @GetMapping("/{serverId}/roles")
    public ResponseEntity<List<Role>> getRoles(@PathVariable String serverId) {
        return ResponseEntity.ok(roleService.getRolesByServer(serverId));
    }

    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<String> deleteRole(@PathVariable String roleId) {
        return ResponseEntity.ok(roleService.deleteRole(roleId));
    }
}