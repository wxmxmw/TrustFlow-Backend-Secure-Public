package com.trustflow.backend.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentParser {

    public List<TextChunk> parsePdf(String filePath) throws IOException {
        PDDocument document = PDDocument.load(new File(filePath));
        PDFTextStripper stripper = new PDFTextStripper();

        List<TextChunk> chunks = new ArrayList<>();
        for (int i = 1; i <= document.getNumberOfPages(); i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);
            String text = stripper.getText(document);
            if (text != null && !text.trim().isEmpty()) {
                chunks.add(new TextChunk(text.trim(), i));
            }
        }
        document.close();
        return chunks;
    }

    public List<TextChunk> parseTxt(String filePath) throws IOException {
        String content = Files.readString(Paths.get(filePath));
        return Arrays.stream(content.split("\n\n"))
                .filter(s -> !s.trim().isEmpty())
                .map(s -> new TextChunk(s.trim(), null))
                .collect(Collectors.toList());
    }

    public List<TextChunk> parse(String filePath, String mimeType) throws IOException {
        if ("application/pdf".equals(mimeType)) {
            return parsePdf(filePath);
        } else if ("text/plain".equals(mimeType)) {
            return parseTxt(filePath);
        } else {
            throw new UnsupportedOperationException("Unsupported file type: " + mimeType);
        }
    }

    public static class TextChunk {
        private final String text;
        private final Integer pageNumber;

        public TextChunk(String text, Integer pageNumber) {
            this.text = text;
            this.pageNumber = pageNumber;
        }

        public String getText() {
            return text;
        }

        public Integer getPageNumber() {
            return pageNumber;
        }
    }
}