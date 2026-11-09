package com.trustflow.backend.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TextSimilarityService {


    public double charBigramCosine(String a, String b) {
        a = normalize(a);
        b = normalize(b);
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        Map<String, Integer> va = bigrams(a);
        Map<String, Integer> vb = bigrams(b);

        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;

        for (Map.Entry<String, Integer> e : va.entrySet()) {
            int x = e.getValue();
            na += (double) x * x;
            Integer y = vb.get(e.getKey());
            if (y != null) dot += (double) x * y;
        }
        for (int y : vb.values()) nb += (double) y * y;

        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**

     * Overlap(A, B) = |A ∩ B| / min(|A|, |B|)

     */
    public double overlapCoefficient(String a, String b) {
        a = normalize(a);
        b = normalize(b);
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        Map<String, Integer> va = bigrams(a);
        Map<String, Integer> vb = bigrams(b);

        double intersection = 0.0;
        double na = 0.0;
        double nb = 0.0;

        for (int count : va.values()) na += count;
        for (int count : vb.values()) nb += count;

        for (Map.Entry<String, Integer> e : va.entrySet()) {
            Integer countInB = vb.get(e.getKey());
            if (countInB != null) {
                intersection += Math.min(e.getValue(), countInB);
            }
        }

        double minLen = Math.min(na, nb);
        if (minLen == 0) return 0.0;
        
        return intersection / minLen;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private static Map<String, Integer> bigrams(String s) {
        Map<String, Integer> m = new HashMap<>();
        if (s.length() == 1) {
            m.put(s, 1);
            return m;
        }
        for (int i = 0; i < s.length() - 1; i++) {
            String g = s.substring(i, i + 2);
            m.put(g, m.getOrDefault(g, 0) + 1);
        }
        return m;
    }
}
