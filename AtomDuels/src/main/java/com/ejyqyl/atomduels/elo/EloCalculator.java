package com.ejyqyl.atomduels.elo;

/**
 * Pure Zero-Sum Elo rating calculator using K = min(K1, K2).
 * Authored by ejyqyl.
 */
public final class EloCalculator {

    private static final int DEFAULT_CALIBRATION_MATCHES = 10;
    private static final int CALIBRATION_K = 40;
    private static final int NORMAL_K = 24;
    private static final int HIGH_ELO_K = 16;
    private static final int MINIMUM_ELO = 100;

    private EloCalculator() {}

    public record EloResult(int deltaA, int deltaB, int newRatingA, int newRatingB) {}

    /**
     * Determines the K-factor for a player based on match count and current rating.
     */
    public static int getKFactor(int rating, int totalMatches) {
        if (totalMatches < DEFAULT_CALIBRATION_MATCHES) {
            return CALIBRATION_K;
        }
        if (rating >= 1800) {
            return HIGH_ELO_K;
        }
        return NORMAL_K;
    }

    /**
     * Calculates expected score for player A against player B.
     */
    public static double calculateExpectedScore(int ratingA, int ratingB) {
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }

    /**
     * Computes the zero-sum rating change for both players.
     *
     * @param ratingA Current rating of player A
     * @param ratingB Current rating of player B
     * @param matchesA Total games played by player A
     * @param matchesB Total games played by player B
     * @param scoreA 1.0 if player A won, 0.5 for draw, 0.0 if player A lost
     * @return EloResult with zero-sum deltas and new ratings
     */
    public static EloResult calculate(int ratingA, int ratingB, int matchesA, int matchesB, double scoreA) {
        int kA = getKFactor(ratingA, matchesA);
        int kB = getKFactor(ratingB, matchesB);
        int k = Math.min(kA, kB);

        double expectedA = calculateExpectedScore(ratingA, ratingB);
        int deltaA = (int) Math.round(k * (scoreA - expectedA));

        // Guarantee at least 1 point change for decisive win/loss
        if (scoreA == 1.0 && deltaA <= 0) {
            deltaA = 1;
        } else if (scoreA == 0.0 && deltaA >= 0) {
            deltaA = -1;
        }

        // Strict zero-sum: deltaB is always -deltaA
        int deltaB = -deltaA;

        int newRatingA = Math.max(MINIMUM_ELO, ratingA + deltaA);
        int newRatingB = Math.max(MINIMUM_ELO, ratingB + deltaB);

        return new EloResult(deltaA, deltaB, newRatingA, newRatingB);
    }
}
