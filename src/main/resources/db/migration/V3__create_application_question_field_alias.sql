CREATE TABLE application_question_field_alias
(
    id               BIGSERIAL PRIMARY KEY,

    question_id      BIGINT                   NOT NULL,

    identifier_type  VARCHAR(30)              NOT NULL,

    field_identifier VARCHAR(255)             NOT NULL,

    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_application_question_field_alias_question
        FOREIGN KEY (question_id)
            REFERENCES application_question (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_application_question_field_alias_identifier
        UNIQUE (identifier_type, field_identifier),

    CONSTRAINT chk_application_question_field_alias_type
        CHECK (
            identifier_type IN (
                                'AUTOMATION_ID',
                                'FIELD_NAME',
                                'FKIT_ID'
                )
            )
);

CREATE INDEX idx_application_question_field_alias_question_id
    ON application_question_field_alias (question_id);
