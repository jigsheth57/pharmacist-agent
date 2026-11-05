package com.healthcare.demo.pharmacist.a2a;

import com.healthcare.demo.pharmacist.PharmacistAgentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class PharmacistA2AController {

    private final PharmacistAgentService pharmacistService;

    public PharmacistA2AController(PharmacistAgentService pharmacistService) {
        this.pharmacistService = pharmacistService;
    }

    /**
     * A2A Protocol: Agent Card Endpoint
     * Remote agents read this to discover identity, capabilities, and protocols.
     */
    @GetMapping("/.well-known/agent-card.json")
    public Map<String, Object> getAgentCard() {
        return Map.of(
            "name", "pharmacist-agent",
            "description", "Specialist Agent for drug-safety, allergy contraindication, and dosage verification.",
            "version", "1.0.0",
            "skills", List.of(
                Map.of(
                    "id", "prescribe-safety-check",
                    "description", "Evaluates patient EHR and cross-checks proposed drug safety and interactions."
                )
            ),
            "endpoints", Map.of(
                "rpc", "http://localhost:8082/a2a"
            )
        );
    }

    public record A2ARequest(String jsonrpc, String method, Map<String, Object> params, String id) {}
    public record A2AResponse(String jsonrpc, Map<String, Object> result, String id) {}

    /**
     * A2A Protocol: JSON-RPC Task Execution Endpoint
     */
    @PostMapping("/a2a")
    public A2AResponse handleA2ATask(@RequestBody A2ARequest request) {
        String taskPrompt = (String) request.params().getOrDefault("query", "");
        
        // Delegate to Ollama ChatClient armed with MCP EHR tools
        String evaluationResult = pharmacistService.reviewPrescription(taskPrompt);

        return new A2AResponse(
            "2.0",
            Map.of(
                "status", "COMPLETED",
                "artifact", evaluationResult
            ),
            request.id()
        );
    }
}