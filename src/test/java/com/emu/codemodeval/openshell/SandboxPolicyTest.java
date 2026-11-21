package com.emu.codemodeval.openshell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Guards the sandbox policy: whoever adds a host must keep it download-only. */
class SandboxPolicyTest {

    @Test
    @SuppressWarnings("unchecked")
    void everyEndpointIsEnforcedReadOnlyHttps() throws IOException {
        Map<String, Object> policy;
        try (InputStream in = getClass().getResourceAsStream("/openshell/sandbox-policy.yaml")) {
            policy = new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
        }
        Map<String, Map<String, Object>> rules = (Map<String, Map<String, Object>>) policy.get("network_policies");
        assertFalse(rules.isEmpty());
        for (Map.Entry<String, Map<String, Object>> rule : rules.entrySet())
            for (Map<String, Object> endpoint : (List<Map<String, Object>>) rule.getValue().get("endpoints")) {
                String where = rule.getKey() + " " + endpoint.get("host");
                assertEquals(443, endpoint.get("port"), where);
                assertEquals("rest", endpoint.get("protocol"), where);
                assertEquals("enforce", endpoint.get("enforcement"), where);
                assertEquals("read-only", endpoint.get("access"), where);
            }
    }
}
