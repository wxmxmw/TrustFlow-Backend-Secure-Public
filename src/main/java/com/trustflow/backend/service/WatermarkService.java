package com.trustflow.backend.service;

import com.trustflow.backend.repo.GenerationRecordRepository;
import com.trustflow.backend.model.GenerationRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WatermarkService {

    private final Map<String, String> watermarkStore = new ConcurrentHashMap<>();

    @Autowired
    private GenerationRecordRepository generationRecordRepository;

    /*
     * =========================

     * =========================
     */
    public byte[] mockGeneratePng(String prompt) throws IOException {
        BufferedImage img = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 512, 512);
        g.setColor(Color.GREEN);
        g.drawString(prompt, 20, 256);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    /*
     * =========================

     * =========================
     */
    public byte[] embedToPng(byte[] pngBytes, String txHash) throws IOException {

        String fingerprint = sha256(pngBytes);


        watermarkStore.put(fingerprint, txHash);


        return pngBytes;
    }

    /*
     * =========================

     * =========================
     */
    public ExtractResult extractFromPng(byte[] pngBytes) {
        try {
            String fingerprint = sha256(pngBytes);


            String txHash = watermarkStore.get(fingerprint);


            if (txHash == null) {
                Optional<GenerationRecord> record = generationRecordRepository.findTop1ByContentHash(fingerprint);
                if (record.isPresent()) {

                    String wmInfo = record.get().getWatermarkInfo();
                    if (wmInfo != null && wmInfo.contains("tx_hash")) {
                        try {


                            int keyIdx = wmInfo.indexOf("tx_hash");
                            if (keyIdx != -1) {
                                int colonIdx = wmInfo.indexOf(":", keyIdx);
                                if (colonIdx != -1) {
                                    int firstQuote = wmInfo.indexOf("\"", colonIdx);
                                    if (firstQuote != -1) {
                                        int secondQuote = wmInfo.indexOf("\"", firstQuote + 1);
                                        if (secondQuote != -1) {
                                            txHash = wmInfo.substring(firstQuote + 1, secondQuote);
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {

                        }
                    }


                    if (txHash == null) {
                        txHash = record.get().getTxHash();
                    }


                    if (txHash == null) {
                        txHash = "record_id_" + record.get().getRecordId();
                    }


                    watermarkStore.put(fingerprint, txHash);
                }
            }

            if (txHash == null) {
                return new ExtractResult(false, null, "no_watermark");
            }
            return new ExtractResult(true, txHash, "success");

        } catch (Exception e) {
            return new ExtractResult(false, null, "error");
        }
    }

    /*
     * =========================

     * =========================
     */
    private String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("sha256_failed", e);
        }
    }

    /*
     * =========================

     * =========================
     */
    public record ExtractResult(
            boolean ok,
            String payload,
            String status) {
    }
}
