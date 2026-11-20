package com.adarsh.job_application_scoring.application.question.service;

import com.adarsh.job_application_scoring.application.question.dto.*;
import com.adarsh.job_application_scoring.application.question.mapper.ApplicationQuestionMapper;
import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestion;
import com.adarsh.job_application_scoring.application.question.model.ApplicationQuestionAlias;
import com.adarsh.job_application_scoring.application.question.repository.ApplicationQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ApplicationQuestionService {

    private final ApplicationQuestionRepository repository;
    private final ApplicationQuestionMapper mapper;
    private final AnswerValueValidator answerValueValidator;

    public ApplicationQuestionService(
            ApplicationQuestionRepository repository,
            ApplicationQuestionMapper mapper,
            AnswerValueValidator answerValueValidator
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.answerValueValidator = answerValueValidator;
    }

    @Transactional
    public ApplicationQuestionResponse createQuestion(
            ApplicationQuestionRequest request
    ) {
        ApplicationQuestion question = createQuestionEntity(
                request,
                new HashSet<>(),
                new HashSet<>()
        );

        return mapper.toResponse(repository.save(question));
    }

    @Transactional
    public List<ApplicationQuestionResponse> createQuestions(
            BulkApplicationQuestionRequest request
    ) {
        Set<String> questionKeys = new HashSet<>();
        Set<String> normalizedAliases = new HashSet<>();

        List<ApplicationQuestionResponse> responses = new ArrayList<>();

        for (ApplicationQuestionRequest questionRequest : request.questions()) {

            ApplicationQuestion question = createQuestionEntity(
                    questionRequest,
                    questionKeys,
                    normalizedAliases
            );

            ApplicationQuestion saved = repository.save(question);
            responses.add(mapper.toResponse(saved));
        }

        return responses;
    }

    private ApplicationQuestion createQuestionEntity(
            ApplicationQuestionRequest request,
            Set<String> batchQuestionKeys,
            Set<String> batchNormalizedAliases
    ) {
        String questionKey = request.questionKey().trim();

        if (repository.existsByQuestionKey(questionKey)
                || !batchQuestionKeys.add(questionKey)) {
            throw new IllegalArgumentException(
                    "Question key already exists: " + questionKey
            );
        }

        answerValueValidator.validate(
                request.answerType(),
                request.answerValue()
        );

        ApplicationQuestion question = new ApplicationQuestion();
        question.setQuestionKey(questionKey);
        question.setCanonicalQuestion(request.canonicalQuestion().trim());
        question.setCategory(request.category().trim());
        question.setAnswerType(request.answerType());
        question.setAnswerValue(request.answerValue());

        addAlias(
                question,
                request.canonicalQuestion(),
                batchNormalizedAliases
        );

        if (request.aliases() != null) {
            for (String alias : request.aliases()) {
                addAlias(question, alias, batchNormalizedAliases);
            }
        }

        return question;
    }

    @Transactional
    public ApplicationQuestionResponse addAlias(
            Long questionId,
            String aliasText
    ) {
        ApplicationQuestion question = getQuestionEntity(questionId);

        addAlias(question, aliasText, new HashSet<>());

        return mapper.toResponse(repository.save(question));
    }

    @Transactional
    public ApplicationQuestionResponse addAliases(
            Long questionId,
            BulkAliasRequest request
    ) {
        ApplicationQuestion question = getQuestionEntity(questionId);

        Set<String> batchAliases = new HashSet<>();

        for (String alias : request.aliases()) {
            addAlias(question, alias, batchAliases);
        }

        return mapper.toResponse(repository.save(question));
    }

    @Transactional
    public ApplicationQuestionResponse updateQuestion(
            Long questionId,
            UpdateApplicationQuestionRequest request
    ) {
        ApplicationQuestion question = getQuestionEntity(questionId);

        String newCanonicalQuestion = request.canonicalQuestion().trim();

        // Preserve the previous canonical wording as an alias.
        addAlias(question, newCanonicalQuestion, new HashSet<>());

        question.setCanonicalQuestion(newCanonicalQuestion);
        question.setCategory(request.category().trim());

        return mapper.toResponse(repository.save(question));
    }

    @Transactional
    public ApplicationQuestionResponse updateAnswer(
            Long questionId,
            UpdateApplicationAnswerRequest request
    ) {
        ApplicationQuestion question = getQuestionEntity(questionId);

        answerValueValidator.validate(
                question.getAnswerType(),
                request.answerValue()
        );

        question.setAnswerValue(request.answerValue());

        return mapper.toResponse(repository.save(question));
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationQuestionResponse> findByQuestionText(
            String questionText
    ) {
        String normalizedText = QuestionNormalizer.normalize(questionText);

        return repository.findByAliasNormalizedText(normalizedText)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ApplicationQuestionResponse getByQuestionKey(String questionKey) {
        return repository.findByQuestionKey(questionKey)
                .map(mapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Question not found: " + questionKey
                ));
    }

    @Transactional(readOnly = true)
    public List<ApplicationQuestionResponse> getAllQuestions() {
        return repository.findAllWithAliases()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationQuestionResponse> getQuestionsByCategory(
            String category
    ) {
        return repository.findAllByCategory(category)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        ApplicationQuestion question = getQuestionEntity(questionId);
        repository.delete(question);
    }

    private ApplicationQuestion getQuestionEntity(Long questionId) {
        return repository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Question not found: " + questionId
                ));
    }

    private void addAlias(
            ApplicationQuestion question,
            String aliasText,
            Set<String> batchNormalizedAliases
    ) {
        String normalizedText = QuestionNormalizer.normalize(aliasText);

        boolean alreadyBelongsToQuestion = question.getAliases()
                .stream()
                .anyMatch(alias ->
                        alias.getNormalizedText().equals(normalizedText)
                );

        if (alreadyBelongsToQuestion) {
            return;
        }

        if (!batchNormalizedAliases.add(normalizedText)) {
            throw new IllegalArgumentException(
                    "Duplicate alias in the current request: " + aliasText
            );
        }

        if (repository.findByAliasNormalizedText(normalizedText).isPresent()) {
            throw new IllegalArgumentException(
                    "This wording is already mapped to another question: "
                            + aliasText
            );
        }

        ApplicationQuestionAlias alias = new ApplicationQuestionAlias();
        alias.setQuestionText(aliasText.trim());
        alias.setNormalizedText(normalizedText);

        question.addAlias(alias);
    }
}