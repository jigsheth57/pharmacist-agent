package com.healthcare.demo.pharmacist;

import com.healthcare.demo.shared.*;
import com.healthcare.demo.shared.Contracts.*;
import java.util.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Read the chart, run mandatory rules, then ask the model to explain that evidence. */
@Service
public class PharmacistAgentService implements Specialist {
  private final String toolsUrl;
  private final ModelAssessment model;

  public PharmacistAgentService(
      ChatClient.Builder builder,
      @Value("${demo.mcp-url}") String toolsUrl,
      @Value("${demo.model}") String modelName) {
    this.toolsUrl = toolsUrl;
    this.model = new ModelAssessment(builder, modelName);
  }

  @Override
  public SpecialistResult review(ReviewInput input, String mcpToken, ActivityLog log) {
    try (var tools = new McpAccess(toolsUrl, mcpToken, log)) {
      var patient =
          tools.call(
              "getPatientSnapshot", Map.of("patientId", input.patientId()), PatientSnapshot.class);
      if (!patient.snapshotVersion().equals(input.snapshotVersion()))
        throw new IllegalStateException("Patient snapshot changed during the review");
      var check =
          tools.call(
              "evaluateMedicationSafety",
              Map.of(
                  "patientId",
                  input.patientId(),
                  "proposedTreatment",
                  input.proposedTreatment(),
                  "reportedMedications",
                  input.reportedMedications() == null ? List.of() : input.reportedMedications()),
              SafetyCheck.class);
      var sources = new ArrayList<>(check.evidence());
      for (var finding : check.findings())
        sources.add(
            new Evidence(
                finding.sourceId(),
                "Patient chart finding",
                patient.snapshotVersion(),
                "medication review",
                finding.message(),
                List.of()));
      if (!check.supported())
        return result(
            input,
            "The required demonstration checks could not be completed.",
            check,
            sources,
            log,
            null);
      var context =
          Map.of(
              "patientId",
              input.patientId(),
              "proposal",
              input.proposedTreatment(),
              "allergies",
              patient.allergies(),
              "currentMedications",
              patient.medications(),
              "reportedMedications",
              input.reportedMedications(),
              "ruleFindings",
              check.findings(),
              "evidence",
              sources);
      var reply = model.assess("pharmacist", context, sources, log);
      return result(input, reply.assessment().summary(), check, sources, log, reply.usage());
    }
  }

  private SpecialistResult result(
      ReviewInput input,
      String summary,
      SafetyCheck check,
      List<Evidence> evidence,
      ActivityLog log,
      ModelUsage usage) {
    return new SpecialistResult(
        "pharmacist",
        input.caseId(),
        input.patientId(),
        input.revision(),
        input.snapshotVersion(),
        summary,
        check.findings(),
        evidence,
        List.of(),
        Map.of(
            "supported",
            String.valueOf(check.supported()),
            "drug",
            check.drug(),
            "scope",
            "Selected demo allergies/interactions only; no dose or diagnosis validation"),
        log.events(),
        usage);
  }
}
