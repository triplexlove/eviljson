package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class JacksonFuzz {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (int d = 0; d <= 15; d++) {
            List<Integer> matches = new ArrayList<>();
            for (int ch = 0; ch <= 0xFFFF; ch++) {
                char variant = (char) ch;
                if (Character.digit(variant, 16) != d) {
                    continue;
                }
                String escaped = "\\u006" + variant;
                String json = "{\"k\":\"" + escaped + "\"}";
                try {
                    JsonNode root = mapper.readTree(json);
                    String res = root.path("k").asText();
                    if (res != null && res.length() == 1 && res.charAt(0) == (char) (0x0060 + d)) {
                        matches.add(ch);
                    }
                } catch (Exception ignore) {}
            }
            String label = d < 10 ? Integer.toString(d) : String.valueOf((char) ('a' + (d - 10)));
            System.out.printf("jackson hex %s replacements: %d%n", label, matches.size());
            for (int i = 0; i < matches.size() && i < 32; i++) {
                int cp = matches.get(i);
                System.out.printf("U+%04X %c%n", cp, (char) cp);
            }
        }
    }
}
