package com.staniul.util.validation;

/**
 * Validator of type. Functional interface that defines validation
 *
 * @param <T> Type of elements that are being validated.
 */
public interface Validator<T> {
    /**
     * Validates if the contents of elements match some rule. Should return {@code true} if it is valid or {@code false}
     * if it is not valid. If there are more then one validator and validator cannot say if it is valid or not then it
     * should assume that element is valid and let another validator to chose. Chain of validation should be accepted if
     * and only if all of the validators returned with {@code true}. That means the element is valid and therefore
     * validation chain should be considered valid.
     *
     * @param element Element that should be validated.
     *
     * @return {@code false} if element is not valid, {@code true} if element is valid or validator cannot distinguish.
     */
    boolean validate(T element);
}
