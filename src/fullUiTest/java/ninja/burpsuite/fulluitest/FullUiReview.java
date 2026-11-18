// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.fulluitest;

import com.google.gson.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Validates user-supplied manual evidence; never generates reviewer approval. */
final class FullUiReview {
    static void run(Path results,Path evidence)throws Exception {
        JsonObject original=JsonParser.parseString(Files.readString(results)).getAsJsonObject();
        JsonObject human=JsonParser.parseString(Files.readString(evidence)).getAsJsonObject();
        if(!original.has("catalog_sha256")||!original.get("catalog_sha256").getAsString().equals(FullUiReport.catalogFingerprint())
                ||!original.get("catalog_sha256").equals(human.get("catalog_sha256")))
            throw new IllegalArgumentException("The UI test plan changed or is missing; rerun the current suite");
        if(!original.get("jar_sha256").equals(human.get("jar_sha256"))||!original.get("host").equals(human.get("host")))
            throw new IllegalArgumentException("Manual evidence must match this jar and host exactly");
        if(!human.has("reviewer")||human.get("reviewer").getAsString().isBlank()||!human.has("tested_utc"))
            throw new IllegalArgumentException("A real reviewer and tested_utc are required; do not invent sign-off");
        Instant tested=Instant.parse(human.get("tested_utc").getAsString());
        Duration age=Duration.between(tested,Instant.now());
        if(age.isNegative()||age.compareTo(Duration.ofDays(7))>0)throw new IllegalArgumentException("Manual evidence must be current (within seven days)");
        Map<String,JsonObject> manual=new HashMap<>();
        Set<String> expected=new HashSet<>();FullUiReport.catalog().stream().filter(c->c.mode().equals("manual")).forEach(c->expected.add(c.id()));
        for(JsonElement item:human.getAsJsonArray("cases")) {
            JsonObject entry=item.getAsJsonObject();String id=entry.get("id").getAsString();
            if(!expected.contains(id)||manual.put(id,entry)!=null)throw new IllegalArgumentException("Unknown or duplicate manual case: "+id);
            if(!Set.of("PASS","NOT_APPLICABLE").contains(entry.get("status").getAsString())||entry.get("evidence").getAsString().isBlank())
                throw new IllegalArgumentException("Each manual case needs a result and concrete evidence: "+id);
        }
        if(!manual.keySet().equals(expected))throw new IllegalArgumentException("Every required manual case must be reviewed");
        Set<String> all=new HashSet<>();
        for(JsonElement item:original.getAsJsonArray("cases")) {
            JsonObject entry=item.getAsJsonObject();String id=entry.get("id").getAsString();
            if(!all.add(id))throw new IllegalArgumentException("Duplicate automated report case: "+id);
            if(manual.containsKey(id)) {entry.add("status",manual.get(id).get("status"));entry.add("evidence",manual.get(id).get("evidence"));}
            else if(!Set.of("PASS","NOT_APPLICABLE").contains(entry.get("status").getAsString()))
                throw new IllegalArgumentException("Automated case is failed/blocked and cannot be waived by manual review: "+id);
        }
        if(!all.equals(new HashSet<>(FullUiReport.catalog().stream().map(FullUiReport.Case::id).toList())))
            throw new IllegalArgumentException("Automated report does not cover the complete current catalog");
        original.addProperty("overall","PASS");original.addProperty("reviewer",human.get("reviewer").getAsString());original.addProperty("manual_tested_utc",tested.toString());
        original.addProperty("manual_evidence_sha256",FullUiReport.sha256(evidence));
        Path output=results.resolveSibling("reviewed-results.json");Files.writeString(output,FullUiReport.JSON.toJson(original));
        System.out.println("Full UI manual review checks passed: "+output+". Other OS/channel runs and release security gates remain separate.");
    }
}
