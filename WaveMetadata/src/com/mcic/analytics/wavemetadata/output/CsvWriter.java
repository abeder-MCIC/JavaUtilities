package com.mcic.analytics.wavemetadata.output;

import com.mcic.analytics.wavemetadata.model.Dashboard;
import com.mcic.analytics.wavemetadata.model.DashboardField;
import com.mcic.analytics.wavemetadata.model.Dataset;
import com.mcic.analytics.wavemetadata.model.DatasetField;
import com.mcic.analytics.wavemetadata.model.JunctionEntry;
import com.mcic.analytics.wavemetadata.util.RetryHandler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

/**
 * Writes CSV artifacts for dashboards, datasets, fields, and junctions.
 * Supports chunked uploads for large payloads.
 */
/**
 * Writes CSV artifacts and uploads via API with chunking.
 */
public class CsvWriter {
    private static final char DEFAULT_SEPARATOR = ',';
    private static final char DEFAULT_QUOTE = '"';
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB
    private final RetryHandler retryHandler = new RetryHandler();

    public void writeDashboards(List<Dashboard> dashboards, Path out) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(out)) {
            writer.write("DashboardName,Application,MasterLabel,Id,CreatedBy,LastModifiedBy");
            writer.newLine();
            for (Dashboard d : dashboards) {
                writer.write(escape(d.getName())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(d.getFolder())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(d.getLabel())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(d.getId())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(d.getCreatedBy())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(d.getLastModifiedBy()));
                writer.newLine();
            }
        }
    }

    /**
     * Writes dataset metadata to a CSV file.
     */
    public void writeDatasets(List<Dataset> datasets, Path out) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(out)) {
            writer.write("DatasetName,MasterLabel,Id,Application");
            writer.newLine();
            for (Dataset ds : datasets) {
                writer.write(escape(ds.getName())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(ds.getLabel())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(ds.getId())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(ds.getContainer()));
                writer.newLine();
            }
        }
    }

    /**
     * Writes extracted dashboard fields to a CSV file.
     */
    public void writeDashboardFields(List<DashboardField> fields, Path out) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(out)) {
            writer.write("DashboardName,StepName,DatasetName,FieldName");
            writer.newLine();
            for (DashboardField f : fields) {
                writer.write(escape(f.getDashboardName())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(f.getStepName())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(f.getDatasetName())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(f.getFieldName()));
                writer.newLine();
            }
        }
    }

    /**
     * Writes dataset fields (from XMD) to a CSV file.
     */
    public void writeDatasetFields(List<DatasetField> fields, Path out) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(out)) {
            writer.write("DatasetName,FieldName,Label,Type");
            writer.newLine();
            for (DatasetField f : fields) {
                writer.write(escape(f.getDatasetName())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(f.getFieldName())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(f.getLabel())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(f.getType()));
                writer.newLine();
            }
        }
    }

    /**
     * Writes dashboard→dataset junction entries to a CSV file.
     */
    public void writeJunctions(List<JunctionEntry> links, Path out) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(out)) {
            writer.write("DashboardName,DatasetName");
            writer.newLine();
            for (JunctionEntry j : links) {
                writer.write(escape(j.getDashboardName())); writer.write(DEFAULT_SEPARATOR);
                writer.write(escape(j.getDatasetName()));
                writer.newLine();
            }
        }
    }

    /**
     * Uploads a CSV file in Base64-encoded chunks (1MB) to the given URL.
     */
    public void uploadWithChunks(Path csvFile, String uploadUrl, String authToken) throws IOException {
        byte[] data = Files.readAllBytes(csvFile);
        String base64 = Base64.getEncoder().encodeToString(data);
        int total = base64.length();
        for (int offset = 0; offset < total; offset += CHUNK_SIZE) {
            int end = Math.min(total, offset + CHUNK_SIZE);
            String chunk = base64.substring(offset, end);
            String payload = "{\"fileData\":\"" + chunk + "\"}";
            retryHandler.executeWithRetry(() -> {
                try (CloseableHttpClient http = HttpClients.createDefault()) {
                    HttpPost post = new HttpPost(uploadUrl);
                    post.setHeader("Authorization", "Bearer " + authToken);
                    post.setHeader("Content-Type", "application/json");
                    post.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));
                    try (CloseableHttpResponse resp = http.execute(post)) {
                        int status = resp.getCode();
                        if (status < 200 || status >= 300) {
                            throw new IOException("Upload chunk failed with status " + status);
                        }
                    }
                }
                return null;
            }, 3);
        }
    }

    /**
     * Escapes a field value for CSV output.
     */
    private String escape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return DEFAULT_QUOTE + escaped + DEFAULT_QUOTE;
    }
}
