package com.carPooling.backend.service;

public interface UploadService {
    void uploadImage(String base64);

    /**
     *
     * @param base64
     * upload doc like pdf, docx etc..
     */
    void uploadDocuments(String base64);
}
