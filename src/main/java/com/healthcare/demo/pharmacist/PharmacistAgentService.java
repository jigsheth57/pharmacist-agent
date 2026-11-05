package com.healthcare.demo.pharmacist;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class PharmacistAgentService {

    private final ChatClient chatClient;

    public PharmacistAgentService(ChatClient.Builder chatClientBuilder, ToolCallbackProvider mcpToolCallbackProvider) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                    You are an expert Clinical Pharmacist Specialist Agent.
                    Your goal is to validate whether a proposed medication is safe for a patient.
                    ALWAYS call the available EHR tools via MCP to look up the patient's medical history,
                    known allergies, and existing medications before rendering your verdict.
                    If an allergy or severe interaction is detected, reject the prescription and suggest a safe alternative.
                    """)
                // Automatically binds all tools discovered from the MCP server
                .defaultTools(mcpToolCallbackProvider.getToolCallbacks())
                .build();
    }

    public String reviewPrescription(String taskDescription) {
        return chatClient.prompt()
                .user(taskDescription)
                .call()
                .content();
    }
}