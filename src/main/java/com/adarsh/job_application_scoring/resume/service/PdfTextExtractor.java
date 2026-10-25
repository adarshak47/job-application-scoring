
package com.adarsh.job_application_scoring.resume.service;

import java.io.IOException;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfTextExtractor {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_PDF_PAGES = 20;
    private static final int PDF_SIGNATURE_LENGTH = 5;
    private static final String PDF_SIGNATURE = "%PDF-";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    public String extractText(MultipartFile file) {
        validateFile(file);

        try {
            byte[] fileBytes = file.getBytes();

            validatePdfSignature(fileBytes);

            try (PDDocument document = Loader.loadPDF(fileBytes)) {
                if (document.isEncrypted()) {
                    throw new IllegalArgumentException(
                            "Password-protected PDFs are not supported."
                    );
                }

                if (document.getNumberOfPages() == 0) {
                    throw new IllegalArgumentException(
                            "The uploaded PDF contains no pages."
                    );
                }

                if (document.getNumberOfPages() > MAX_PDF_PAGES) {
                    throw new IllegalArgumentException(
                            "The uploaded PDF exceeds the maximum allowed limit of "
                                    + MAX_PDF_PAGES + " pages."
                    );
                }

                PDFTextStripper textStripper = new PDFTextStripper();
                textStripper.setSortByPosition(true);

                String extractedText = textStripper.getText(document)
                        .replace("\r\n", "\n")
                        .replace('\r', '\n')
                        .replace('\u00A0', ' ')
                        .strip();

                if (extractedText.isBlank()) {
                    throw new IllegalArgumentException(
                            "No extractable text was found in the PDF. "
                                    + "Scanned PDFs require OCR, which is not supported yet."
                    );
                }

                return extractedText;
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Unable to read the uploaded PDF. "
                            + "Please upload a valid, readable PDF file.",
                    exception
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please upload a non-empty PDF file."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "The uploaded PDF exceeds the maximum allowed size of 5 MB."
            );
        }

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        boolean hasPdfFilename = filename != null
                && filename.toLowerCase(Locale.ROOT).endsWith(".pdf");

        boolean hasPdfContentType = contentType != null
                && contentType.toLowerCase(Locale.ROOT)
                .startsWith(PDF_CONTENT_TYPE);

        if (!hasPdfFilename && !hasPdfContentType) {
            throw new IllegalArgumentException(
                    "Please upload a PDF file."
            );
        }
    }

    private void validatePdfSignature(byte[] fileBytes) {
        if (fileBytes.length < PDF_SIGNATURE_LENGTH) {
            throw new IllegalArgumentException(
                    "The uploaded file is not a valid PDF."
            );
        }

        String signature = new String(
                fileBytes,
                0,
                PDF_SIGNATURE_LENGTH,
                java.nio.charset.StandardCharsets.US_ASCII
        );

        if (!PDF_SIGNATURE.equals(signature)) {
            throw new IllegalArgumentException(
                    "The uploaded file does not contain a valid PDF signature."
            );
        }
    }
}
