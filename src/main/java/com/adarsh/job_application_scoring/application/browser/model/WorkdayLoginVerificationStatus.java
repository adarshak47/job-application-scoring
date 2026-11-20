package com.adarsh.job_application_scoring.application.browser.model;

public enum WorkdayLoginVerificationStatus {

    LOGIN_SUCCESSFULLY_VERIFIED,

    LOGIN_FORM_STILL_VISIBLE,

    INVALID_CREDENTIALS,

    MFA_REQUIRED,

    CAPTCHA_REQUIRED,

    AUTHENTICATION_NOT_VERIFIED
}