CREATE TABLE application_question
(
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    question_key       VARCHAR(150) NOT NULL UNIQUE,
    canonical_question TEXT         NOT NULL,

    category           VARCHAR(50)  NOT NULL,
    answer_type        VARCHAR(30)  NOT NULL,
    answer_value       JSONB        NOT NULL,

    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_application_answer_type
        CHECK (answer_type IN (
                               'TEXT',
                               'BOOLEAN',
                               'NUMBER',
                               'DATE',
                               'SINGLE_SELECT',
                               'MULTI_SELECT'
            ))
);

CREATE TABLE application_question_alias
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    question_id     BIGINT    NOT NULL,
    question_text   TEXT      NOT NULL,
    normalized_text TEXT      NOT NULL UNIQUE,

    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_question_alias
        FOREIGN KEY (question_id)
            REFERENCES application_question (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_question_alias_question_id
    ON application_question_alias (question_id);