
package com.adarsh.job_application_scoring.application.browser.service;

import com.adarsh.job_application_scoring.application.browser.dto.WorkdayResumeUploadStepResult;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayApplicationData;
import com.adarsh.job_application_scoring.application.data.dto.WorkdayResumeData;
import com.adarsh.job_application_scoring.application.data.service.WorkdayApplicationDataProvider;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
public class WorkdayResumeUploadStepHandler {

    private static final String RESUME_SECTION_SELECTOR =
            "[role='group'][aria-labelledby='Resume/CV-section']";

    private static final String FILE_INPUT_SELECTOR =
            "input[data-automation-id='file-upload-input-ref']";

    private static final long MAX_FILE_SIZE_BYTES =
            5L * 1024 * 1024;

    private final WorkdayApplicationDataProvider workdayApplicationDataProvider;

    public WorkdayResumeUploadStepHandler(
            WorkdayApplicationDataProvider workdayApplicationDataProvider
    ) {
        this.workdayApplicationDataProvider = workdayApplicationDataProvider;
    }

    public WorkdayResumeUploadStepResult process(Page page) {

        WorkdayApplicationData applicationData =
                workdayApplicationDataProvider.getApplicationData();

        WorkdayResumeData resume = applicationData.resume();

        if (resume == null || resume.filePath() == null) {
            return result(
                    false,
                    "RESUME_NOT_AVAILABLE",
                    null,
                    0,
                    "Candidate profile does not contain a resume file."
            );
        }

        Path resumePath = resume.filePath();

        if (!Files.exists(resumePath) || !Files.isRegularFile(resumePath)) {
            return result(
                    false,
                    "RESUME_FILE_NOT_FOUND",
                    resume.fileName(),
                    0,
                    "Resume file does not exist at: " + resumePath
            );
        }

        try {

            long fileSize = Files.size(resumePath);

            if (fileSize <= 0) {
                return result(
                        false,
                        "RESUME_FILE_EMPTY",
                        resume.fileName(),
                        fileSize,
                        "Resume file is empty."
                );
            }

            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return result(
                        false,
                        "RESUME_FILE_EXCEEDS_SIZE_LIMIT",
                        resume.fileName(),
                        fileSize,
                        "Resume exceeds Workday's 5 MB upload limit."
                );
            }

            Locator section = page.locator(RESUME_SECTION_SELECTOR);

            if (section.count() != 1) {
                return result(
                        false,
                        "RESUME_SECTION_NOT_FOUND",
                        resume.fileName(),
                        fileSize,
                        "Could not uniquely locate the Resume/CV section."
                );
            }

            Locator fileInput = section.locator(FILE_INPUT_SELECTOR);

            if (fileInput.count() != 1) {
                return result(
                        false,
                        "RESUME_FILE_INPUT_NOT_FOUND",
                        resume.fileName(),
                        fileSize,
                        "Could not uniquely locate the resume file input."
                );
            }

            // Upload the candidate's existing resume.
            fileInput.setInputFiles(resumePath);

            // Workday displays an attachment-specific Delete button
            // after the file has been uploaded.
            Locator uploadedAttachment = section.getByRole(
                    com.microsoft.playwright.options.AriaRole.BUTTON,
                    new Locator.GetByRoleOptions()
                            .setName("Delete " + resumePath.getFileName())
                            .setExact(true)
            );

            uploadedAttachment.waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
            );

            if (uploadedAttachment.count() != 1) {
                return result(
                        false,
                        "RESUME_UPLOAD_VERIFICATION_FAILED",
                        resume.fileName(),
                        fileSize,
                        "Uploaded attachment could not be uniquely verified."
                );
            }

            log.info(
                    "Resume uploaded and verified successfully: {}",
                    resumePath.getFileName()
            );

            return result(
                    true,
                    "RESUME_UPLOADED_AND_VERIFIED",
                    resume.fileName(),
                    fileSize,
                    "Workday displayed the uploaded attachment's Delete button."
            );

        } catch (Exception e) {

            log.error("Failed to upload or verify candidate resume", e);

            return result(
                    false,
                    "RESUME_UPLOAD_FAILED",
                    resume.fileName(),
                    safeFileSize(resumePath),
                    e.getMessage()
            );
        }
    }

    private long safeFileSize(Path path) {
        try {
            return Files.exists(path) ? Files.size(path) : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private WorkdayResumeUploadStepResult result(
            boolean successful,
            String status,
            String fileName,
            long fileSize,
            String signal
    ) {
        return new WorkdayResumeUploadStepResult(
                successful,
                status,
                fileName,
                fileSize,
                List.of(
                        signal == null
                                ? "No additional details available."
                                : signal
                )
        );
    }
}
