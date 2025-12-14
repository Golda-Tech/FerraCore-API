package com.goldatech.authservice.web.exception;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;


public class PatternOrEmptyValidator implements ConstraintValidator<PatternOrEmpty, String> {
    private Pattern pattern;

    @Override
    public void initialize(PatternOrEmpty constraintAnnotation) {
        pattern = Pattern.compile(constraintAnnotation.regexp());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isEmpty() || pattern.matcher(value).matches();
    }
}