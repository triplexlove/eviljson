package com.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;

import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) {
        if (args.length > 0) {
            String inputJson = args[0];
            if (inputJson.startsWith("@")) {
                try {
                    String path = inputJson.substring(1);
                    inputJson = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            String obfuscated = obfuscateJsonWithUnicodeVariants(inputJson, new Random());
            System.out.println(obfuscated);
            Object parsed = JSON.parse(obfuscated);
            System.out.println(JSON.toJSONString(parsed));
            return;
        }

        String text = "{\"aa\":\"123\"}";
        JSONObject obj = JSON.parseObject(text);
        System.out.println(obj.getString("aa"));
        System.out.println(obj);
        System.out.println(JSON.parseObject("{\"expression\":\"\\u0054\"}"));

        for (int d = 0; d <= 15; d++) {
            List<Integer> matches = new ArrayList<>();
            for (int ch = 0; ch <= 0xFFFF; ch++) {
                char variant = (char) ch;
                if (Character.digit(variant, 16) != d) {
                    continue;
                }
                String escaped = "\\u006" + variant;
                try {
                    String json = "{\"k\":\"" + escaped + "\"}";
                    String res = JSON.parseObject(json).getString("k");
                    if (res != null && res.length() == 1 && res.charAt(0) == (char) (0x0060 + d)) {
                        matches.add(ch);
                    }
                } catch (Exception ignore) {}
            }
            String label = d < 10 ? Integer.toString(d) : String.valueOf((char) ('a' + (d - 10)));
            System.out.printf("hex %s replacements: %d%n", label, matches.size());
            for (int i = 0; i < matches.size() && i < 32; i++) {
                int cp = matches.get(i);
                System.out.printf("U+%04X %c%n", cp, (char) cp);
            }
        }
    }

    private static List<Character>[] buildHexVariants() {
        @SuppressWarnings("unchecked")
        List<Character>[] variants = new List[16];
        for (int i = 0; i < 16; i++) variants[i] = new ArrayList<>();
        for (int ch = 0; ch <= 0xFFFF; ch++) {
            char c = (char) ch;
            int d = Character.digit(c, 16);
            if (d >= 0) variants[d].add(c);
        }
        return variants;
    }

    private static final List<Character>[] HEX_VARIANTS = buildHexVariants();

    private static char pickVariant(int nibble, Random rnd) {
        List<Character> list = HEX_VARIANTS[nibble];
        return list.get(rnd.nextInt(list.size()));
    }

    private static String escapeCharWithVariants(char ch, Random rnd) {
        int code = ch;
        int n1 = (code >>> 12) & 0xF;
        int n2 = (code >>> 8) & 0xF;
        int n3 = (code >>> 4) & 0xF;
        int n4 = code & 0xF;
        return "\\u" + new String(new char[]{
                pickVariant(n1, rnd),
                pickVariant(n2, rnd),
                pickVariant(n3, rnd),
                pickVariant(n4, rnd)
        });
    }

    private static String escapeCodePointWithVariants(int cp, Random rnd) {
        if (cp <= 0xFFFF) {
            return escapeCharWithVariants((char) cp, rnd);
        } else {
            char[] surrogates = Character.toChars(cp);
            StringBuilder sb = new StringBuilder();
            for (char s : surrogates) sb.append(escapeCharWithVariants(s, rnd));
            return sb.toString();
        }
    }

    private static String unicodeWithVariants(String s, Random rnd) {
        StringBuilder sb = new StringBuilder(s.length() * 6);
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            sb.append(escapeCodePointWithVariants(cp, rnd));
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    private static String obfuscateJsonWithUnicodeVariants(String json, Random rnd) {
        Object obj = JSON.parse(json);
        StringBuilder sb = new StringBuilder(json.length() * 2);
        writeJsonWithVariants(obj, sb, rnd);
        return sb.toString();
    }

    private static void writeJsonWithVariants(Object obj, StringBuilder sb, Random rnd) {
        if (obj == null) {
            sb.append("null");
            return;
        }
        if (obj instanceof JSONObject) {
            JSONObject jo = (JSONObject) obj;
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : jo.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(unicodeWithVariants(e.getKey(), rnd)).append('"');
                sb.append(':');
                writeJsonWithVariants(e.getValue(), sb, rnd);
            }
            sb.append('}');
        } else if (obj instanceof JSONArray) {
            JSONArray arr = (JSONArray) obj;
            sb.append('[');
            for (int i = 0; i < arr.size(); i++) {
                if (i > 0) sb.append(',');
                writeJsonWithVariants(arr.get(i), sb, rnd);
            }
            sb.append(']');
        } else if (obj instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) obj;
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(unicodeWithVariants(e.getKey(), rnd)).append('"');
                sb.append(':');
                writeJsonWithVariants(e.getValue(), sb, rnd);
            }
            sb.append('}');
        } else if (obj instanceof List) {
            List<?> arr = (List<?>) obj;
            sb.append('[');
            for (int i = 0; i < arr.size(); i++) {
                if (i > 0) sb.append(',');
                writeJsonWithVariants(arr.get(i), sb, rnd);
            }
            sb.append(']');
        } else if (obj instanceof String) {
            String s = (String) obj;
            sb.append('"').append(unicodeWithVariants(s, rnd)).append('"');
        } else if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(String.valueOf(obj));
        } else {
            sb.append('"').append(unicodeWithVariants(String.valueOf(obj), rnd)).append('"');
        }
    }
}
