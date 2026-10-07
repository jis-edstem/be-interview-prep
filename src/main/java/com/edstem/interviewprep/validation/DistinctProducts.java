package com.edstem.interviewprep.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DistinctProductsValidator.class)
public @interface DistinctProducts {

    String message() default "must not repeat a product";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
