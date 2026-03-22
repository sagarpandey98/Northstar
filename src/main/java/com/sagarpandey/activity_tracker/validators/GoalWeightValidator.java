package com.sagarpandey.activity_tracker.validators;

public class GoalWeightValidator {

    private GoalWeightValidator() {}

    /**
     * Validates health score weights from a GoalRequest.
     * Rule: if ANY of the three weights is provided,
     * ALL THREE must be provided and must sum to 100.
     *
     * @throws IllegalArgumentException with clear message if invalid
     */
    public static void validateWeights(
            Integer consistencyWeight,
            Integer momentumWeight,
            Integer progressWeight) {

        boolean anyProvided = consistencyWeight != null
            || momentumWeight != null
            || progressWeight != null;

        if (!anyProvided) {
            return;
        }

        boolean allProvided = consistencyWeight != null
            && momentumWeight != null
            && progressWeight != null;

        if (!allProvided) {
            throw new IllegalArgumentException(
                "If providing weights, all three must be specified: " +
                "consistencyWeight, momentumWeight, and progressWeight"
            );
        }

        if (consistencyWeight < 0 || momentumWeight < 0
                || progressWeight < 0) {
            throw new IllegalArgumentException(
                "All weights must be non-negative"
            );
        }

        int sum = consistencyWeight + momentumWeight + progressWeight;
        if (sum != 100) {
            throw new IllegalArgumentException(
                "Weights must sum to 100. " +
                "Provided: consistencyWeight=" + consistencyWeight +
                ", momentumWeight=" + momentumWeight +
                ", progressWeight=" + progressWeight +
                ", sum=" + sum
            );
        }
    }
}
