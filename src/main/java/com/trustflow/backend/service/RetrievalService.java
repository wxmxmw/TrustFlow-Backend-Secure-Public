package com.trustflow.backend.service;

import com.trustflow.backend.repo.DocumentChunkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RetrievalService {

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final int defaultTopK;
    private final double similarityThreshold;

    public RetrievalService(DocumentChunkRepository chunkRepository,
            EmbeddingService embeddingService,
            @Value("${app.rag.top-k:5}") int defaultTopK,
            @Value("${app.rag.similarity-threshold:0.4}") double similarityThreshold) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.defaultTopK = defaultTopK;
        this.similarityThreshold = similarityThreshold;
    }

    public List<RetrievedChunk> retrieveTopK(String query, Long userId) {
        return retrieveTopK(query, userId, defaultTopK);
    }

    public List<RetrievedChunk> retrieveTopK(String query, Long userId, int topK) {

        float[] queryVector = embeddingService.generateEmbedding(query);


        String vectorStr = formatVector(queryVector);



        List<Object[]> results = chunkRepository.findSimilarChunks(vectorStr, userId, topK);


        return results.stream()
                .map(this::convertToChunkResult)
                .filter(chunk -> chunk.getSimilarityScore() >= similarityThreshold)
                .toList();
    }

    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private RetrievedChunk convertToChunkResult(Object[] result) {

        // [0] chunk_id (BigInt -> Long)
        // [1] doc_id (BigInt -> Long)
        // [2] content_text (String)
        // [3] page_number (Integer)
        // [4] similarity (Double/Float)

        Long chunkId = ((Number) result[0]).longValue();
        Long docId = ((Number) result[1]).longValue();
        String content = (String) result[2];
        Integer pageNumber = result[3] != null ? ((Number) result[3]).intValue() : null;
        Float similarity = ((Number) result[4]).floatValue();
        String chunkHash = (String) result[5];
        String fileHash = (String) result[6];

        return new RetrievedChunk(
                chunkId,
                docId,
                content,
                pageNumber,
                similarity,
                chunkHash,
                fileHash);
    }


    public static class RetrievedChunk {
        private final Long chunkId;
        private final Long docId;
        private final String content;
        private final Integer pageNumber;
        private final Float similarityScore;
        private final String chunkHash;
        private final String fileHash;

        public RetrievedChunk(Long chunkId, Long docId, String content,
                Integer pageNumber, Float similarityScore,
                String chunkHash, String fileHash) {
            this.chunkId = chunkId;
            this.docId = docId;
            this.content = content;
            this.pageNumber = pageNumber;
            this.similarityScore = similarityScore;
            this.chunkHash = chunkHash;
            this.fileHash = fileHash;
        }

        public Long getChunkId() {
            return chunkId;
        }

        public Long getDocId() {
            return docId;
        }

        public String getContent() {
            return content;
        }

        public Integer getPageNumber() {
            return pageNumber;
        }

        public Float getSimilarityScore() {
            return similarityScore;
        }

        public String getChunkHash() {
            return chunkHash;
        }

        public String getFileHash() {
            return fileHash;
        }
    }
}
