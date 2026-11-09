package com.trustflow.backend.repo;

import com.trustflow.backend.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {


  List<DocumentChunk> findByDocId(Long docId);


  @Modifying
  @Query("DELETE FROM DocumentChunk dc WHERE dc.docId = :docId")
  void deleteByDocId(@Param("docId") Long docId);

  /**




   */
  @Modifying
  @Query(value = "UPDATE document_chunks SET embedding_vector = CAST(:vectorStr AS vector) WHERE chunk_id = :chunkId", nativeQuery = true)
  void updateEmbeddingVector(@Param("chunkId") Long chunkId, @Param("vectorStr") String vectorStr);

  /**

   * 





   */
  @Query(value = """
      SELECT
          c.chunk_id,      -- [0]
          c.doc_id,        -- [1]
          c.content_text,  -- [2]
          c.page_number,   -- [3]
          1 - (c.embedding_vector <=> cast(:queryVector as vector)) as similarity, -- [4]
          c.chunk_hash,
          d.file_hash
      FROM document_chunks c
      JOIN documents d ON c.doc_id = d.doc_id
      WHERE d.user_id = :userId
        AND d.status = 'indexed'
      ORDER BY c.embedding_vector <=> cast(:queryVector as vector) ASC
      LIMIT :limit
      """, nativeQuery = true)
  List<Object[]> findSimilarChunks(@Param("queryVector") String queryVector,
      @Param("userId") Long userId,
      @Param("limit") int limit);
}
