package com.trustflow.backend.dto;

public class CitationDto {
    private Long doc_id;
    private Long chunk_id;
    private String file_name;
    private Integer page;
    private Double score;
    private String text_snippet;
    private String url;
    private String download_url;
    private String file_hash;
    private String chunk_hash;
    private String content_snippet;

    public Long getDoc_id() { return doc_id; }
    public void setDoc_id(Long doc_id) { this.doc_id = doc_id; }

    public Long getChunk_id() { return chunk_id; }
    public void setChunk_id(Long chunk_id) { this.chunk_id = chunk_id; }

    public String getFile_name() { return file_name; }
    public void setFile_name(String file_name) { this.file_name = file_name; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getText_snippet() { return text_snippet; }
    public void setText_snippet(String text_snippet) { this.text_snippet = text_snippet; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDownload_url() { return download_url; }
    public void setDownload_url(String download_url) { this.download_url = download_url; }

    public String getFile_hash() { return file_hash; }
    public void setFile_hash(String file_hash) { this.file_hash = file_hash; }

    public String getChunk_hash() { return chunk_hash; }
    public void setChunk_hash(String chunk_hash) { this.chunk_hash = chunk_hash; }

    public String getContent_snippet() { return content_snippet; }
    public void setContent_snippet(String content_snippet) { this.content_snippet = content_snippet; }
}
