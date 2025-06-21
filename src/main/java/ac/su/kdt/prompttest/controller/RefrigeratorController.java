package ac.su.kdt.prompttest.controller;

import ac.su.kdt.prompttest.dto.RefrigeratorDTO;
import ac.su.kdt.prompttest.dto.RefrigeratorRequestDTO;
import ac.su.kdt.prompttest.service.RefrigeratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refrigerators")
@RequiredArgsConstructor
public class RefrigeratorController {
    
    private final RefrigeratorService refrigeratorService;
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RefrigeratorDTO>> getUserRefrigerators(@PathVariable Integer userId) {
        List<RefrigeratorDTO> refrigerators = refrigeratorService.getUserRefrigerators(userId);
        return ResponseEntity.ok(refrigerators);
    }
    
    @GetMapping("/{refrigeratorId}")
    public ResponseEntity<RefrigeratorDTO> getRefrigerator(@PathVariable Integer refrigeratorId) {
        RefrigeratorDTO refrigerator = refrigeratorService.getRefrigerator(refrigeratorId);
        return ResponseEntity.ok(refrigerator);
    }
    
    @PostMapping
    public ResponseEntity<RefrigeratorDTO> createRefrigerator(@RequestBody RefrigeratorRequestDTO request) {
        RefrigeratorDTO createdRefrigerator = refrigeratorService.createRefrigerator(request.getUserId(), request);
        return ResponseEntity.ok(createdRefrigerator);
    }
    
    @GetMapping
    public ResponseEntity<List<RefrigeratorDTO>> getAllRefrigerators() {
        List<RefrigeratorDTO> refrigerators = refrigeratorService.getAllRefrigerators();
        return ResponseEntity.ok(refrigerators);
    }
    
    @PostMapping("/user/{userId}")
    public ResponseEntity<RefrigeratorDTO> createRefrigeratorForUser(
            @PathVariable Integer userId,
            @RequestBody RefrigeratorRequestDTO requestDTO) {
        RefrigeratorDTO refrigerator = refrigeratorService.createRefrigerator(userId, requestDTO);
        return ResponseEntity.ok(refrigerator);
    }
    
    @PutMapping("/{refrigeratorId}")
    public ResponseEntity<RefrigeratorDTO> updateRefrigerator(
            @PathVariable Integer refrigeratorId,
            @RequestBody RefrigeratorRequestDTO requestDTO) {
        RefrigeratorDTO refrigerator = refrigeratorService.updateRefrigerator(refrigeratorId, requestDTO);
        return ResponseEntity.ok(refrigerator);
    }
    
    @DeleteMapping("/{refrigeratorId}")
    public ResponseEntity<Void> deleteRefrigerator(@PathVariable Integer refrigeratorId) {
        refrigeratorService.deleteRefrigerator(refrigeratorId);
        return ResponseEntity.noContent().build();
    }
} 