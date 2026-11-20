-- =========================================
-- Candidate Profile
-- =========================================

CREATE TABLE candidate_profile
(
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- Legal Name
    legal_name_prefix           VARCHAR(30),
    legal_given_name            VARCHAR(150) NOT NULL,
    legal_family_name           VARCHAR(150) NOT NULL,
    legal_local_given_name      VARCHAR(150),
    legal_local_family_name     VARCHAR(150),

    -- Preferred Name
    has_preferred_name          BOOLEAN      NOT NULL DEFAULT FALSE,
    preferred_name_prefix       VARCHAR(30),
    preferred_given_name        VARCHAR(150),
    preferred_family_name       VARCHAR(150),
    preferred_local_given_name  VARCHAR(150),
    preferred_local_family_name VARCHAR(150),

    -- Personal Information
    country                     VARCHAR(100) NOT NULL,
    email                       VARCHAR(255) NOT NULL,
    phone_device_type           VARCHAR(30),
    phone_country_code          VARCHAR(10),
    phone_number                VARCHAR(30),
    phone_extension             VARCHAR(15),

    -- Address
    address_line1               VARCHAR(255),
    address_line2               VARCHAR(255),
    address_line3               VARCHAR(255),
    city                        VARCHAR(100),
    state                       VARCHAR(100),
    postal_code                 VARCHAR(20),
    address_country             VARCHAR(100),

    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =========================================
-- Work Experience
-- =========================================

CREATE TABLE work_experience
(
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id           BIGINT  NOT NULL,

    job_title              VARCHAR(200),
    company                VARCHAR(200),
    location               VARCHAR(200),
    currently_working_here BOOLEAN NOT NULL DEFAULT FALSE,
    start_date             DATE,
    end_date               DATE,
    role_description       TEXT,

    CONSTRAINT fk_work_experience_candidate
        FOREIGN KEY (candidate_id)
            REFERENCES candidate_profile (id)
            ON DELETE CASCADE
);


-- =========================================
-- Education
-- =========================================

CREATE TABLE education
(
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id         BIGINT NOT NULL,

    school_or_university VARCHAR(255),
    degree               VARCHAR(150),
    field_of_study       VARCHAR(150),
    cgpa                 VARCHAR(20),
    start_date           DATE,
    end_date             DATE,

    CONSTRAINT fk_education_candidate
        FOREIGN KEY (candidate_id)
            REFERENCES candidate_profile (id)
            ON DELETE CASCADE
);


-- =========================================
-- Certifications
-- =========================================

CREATE TABLE certification
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id BIGINT NOT NULL,

    name         VARCHAR(255),
    issue_date   DATE,
    expiry_date  DATE,
    file_path    VARCHAR(1000),

    CONSTRAINT fk_certification_candidate
        FOREIGN KEY (candidate_id)
            REFERENCES candidate_profile (id)
            ON DELETE CASCADE
);


-- =========================================
-- Languages
-- =========================================

CREATE TABLE candidate_language
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id BIGINT       NOT NULL,

    language     VARCHAR(100) NOT NULL,
    fluent       BOOLEAN      NOT NULL DEFAULT FALSE,
    proficiency  VARCHAR(50),

    CONSTRAINT fk_language_candidate
        FOREIGN KEY (candidate_id)
            REFERENCES candidate_profile (id)
            ON DELETE CASCADE
);


-- =========================================
-- Skills
-- =========================================

CREATE TABLE candidate_skill
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id BIGINT       NOT NULL,

    name         VARCHAR(150) NOT NULL,

    CONSTRAINT fk_skill_candidate
        FOREIGN KEY (candidate_id)
            REFERENCES candidate_profile (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_candidate_skill
        UNIQUE (candidate_id, name)
);


-- =========================================
-- Websites
-- =========================================

CREATE TABLE candidate_website
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id BIGINT NOT NULL,

    name         VARCHAR(150),
    url          VARCHAR(1000),

    CONSTRAINT fk_website_candidate
        FOREIGN KEY (candidate_id)
            REFERENCES candidate_profile (id)
            ON DELETE CASCADE
);


-- =========================================
-- Social Networks
-- =========================================

CREATE TABLE candidate_social_network
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id BIGINT NOT NULL,

    platform     VARCHAR(100),
    url          VARCHAR(1000),

    CONSTRAINT fk_social_network_candidate
        FOREIGN KEY (candidate_id)
            REFERENCES candidate_profile (id)
            ON DELETE CASCADE
);


-- =========================================
-- Resume Document
-- =========================================

CREATE TABLE resume_document
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id BIGINT        NOT NULL UNIQUE,

    file_name    VARCHAR(255)  NOT NULL,
    file_path    VARCHAR(1000) NOT NULL,

    CONSTRAINT fk_resume_candidate
        FOREIGN KEY (candidate_id)
            REFERENCES candidate_profile (id)
            ON DELETE CASCADE
);