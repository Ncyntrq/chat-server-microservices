package com.chatsever.server.controller;

import com.chatsever.server.model.Server;
import com.chatsever.server.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {
    private final ServerService serverService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Server s, @RequestHeader("X-User-Id") String uid) {
        return ResponseEntity.ok(serverService.createServer(s, uid));
    }

    @GetMapping
    public ResponseEntity<?> getMy(@RequestHeader("X-User-Id") String uid) {
        return ResponseEntity.ok(serverService.getMyServers(uid));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetails(@PathVariable Long id) {
        return ResponseEntity.ok(serverService.getServerDetails(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Server s, @RequestHeader("X-User-Id") String uid) {
        return ResponseEntity.ok(serverService.updateServer(id, s, uid));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @RequestHeader("X-User-Id") String uid) {
        serverService.deleteServer(id, uid);
        return ResponseEntity.ok(Map.of("message", "Đã xóa server thành công"));
    }

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestParam String code, @RequestHeader("X-User-Id") String uid) {
        serverService.joinServer(code, uid);
        return ResponseEntity.ok(Map.of("message", "Gia nhập thành công"));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leave(@PathVariable Long id, @RequestHeader("X-User-Id") String uid) {
        serverService.leaveServer(id, uid);
        return ResponseEntity.ok(Map.of("message", "Đã rời khỏi server"));
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<?> createInvite(@PathVariable Long id, @RequestHeader("X-User-Id") String uid) {
        return ResponseEntity.ok(Map.of("inviteCode", serverService.generateNewInviteCode(id, uid)));
    }
}