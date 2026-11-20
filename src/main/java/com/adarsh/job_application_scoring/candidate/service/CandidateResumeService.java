package com.adarsh.job_application_scoring.candidate.service;

import com.adarsh.job_application_scoring.candidate.dto.CandidateProfileResponse;
import com.adarsh.job_application_scoring.candidate.mapper.CandidateProfileMapper;
import com.adarsh.job_application_scoring.candidate.model.CandidateProfile;
import com.adarsh.job_application_scoring.candidate.model.ResumeDocument;
import com.adarsh.job_application_scoring.candidate.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CandidateResumeService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateProfileMapper candidateProfileMapper;
    private final ResumeStorageService resumeStorageService;

    @Transactional
    public CandidateProfileResponse upload(MultipartFile file) {

        CandidateProfile profile = candidateProfileRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "Create candidate profile before uploading a resume."
                ));

        ResumeStorageService.StoredResume storedResume =
                resumeStorageService.store(file);

        ResumeDocument previousResume = profile.getResume();
        String previousFilePath = previousResume == null
                ? null
                : previousResume.getFilePath();

        ResumeDocument newResume = new ResumeDocument();
        newResume.setFileName(storedResume.originalFileName());
        newResume.setFilePath(storedResume.storedFileName());

        profile.setResumeDocument(newResume);

        try {
            CandidateProfile savedProfile =
                    candidateProfileRepository.save(profile);

            registerFileCleanup(
                    storedResume.storedFileName(),
                    previousFilePath
            );

            return candidateProfileMapper.toResponse(savedProfile);

        } catch (RuntimeException exception) {
            resumeStorageService.delete(storedResume.storedFileName());
            throw exception;
        }
    }

    private void registerFileCleanup(
            String newFilePath,
            String previousFilePath
    ) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        if (previousFilePath != null) {
                            resumeStorageService.delete(previousFilePath);
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            resumeStorageService.delete(newFilePath);
                        }
                    }
                }
        );
    }
}