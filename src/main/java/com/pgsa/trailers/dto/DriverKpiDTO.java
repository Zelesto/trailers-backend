package com.pgsa.trailers.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DriverKpiDTO
 *
 * Enhanced for analytics with additional calculated fields
 */
public record DriverKpiDTO(
        String driver,
        BigDecimal totalKm,
        BigDecimal fuelCost,
        Integer tripsCompleted,
        BigDecimal totalRevenue,
        BigDecimal totalCost,
        BigDecimal profit,
        BigDecimal efficiencyScore
) {

        // Compact canonical constructor for null safety
        public DriverKpiDTO {
                if (totalKm == null) totalKm = BigDecimal.ZERO;
                if (fuelCost == null) fuelCost = BigDecimal.ZERO;
                if (tripsCompleted == null) tripsCompleted = 0;
                if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
                if (totalCost == null) totalCost = BigDecimal.ZERO;
                if (profit == null) profit = BigDecimal.ZERO;
                if (efficiencyScore == null) efficiencyScore = BigDecimal.ZERO;
        }

        // Convenience constructor for placeholder driver with trips only
        public DriverKpiDTO(String driver, int tripsCompleted) {
                this(driver,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        tripsCompleted,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);
        }

        // Convenience constructor with full analytics data
        public DriverKpiDTO(String driver,
                            BigDecimal totalKm,
                            BigDecimal fuelCost,
                            Integer tripsCompleted,
                            BigDecimal totalRevenue,
                            BigDecimal totalCost,
                            BigDecimal profit) {
                this(driver,
                        totalKm,
                        fuelCost,
                        tripsCompleted,
                        totalRevenue,
                        totalCost,
                        profit,
                        calculateEfficiencyScore(totalKm, fuelCost));
        }

        // Static factory method for analytics
        public static DriverKpiDTO createForAnalytics(
                String driverName,
                BigDecimal totalKm,
                BigDecimal fuelCost,
                Integer tripsCompleted,
                BigDecimal totalRevenue,
                BigDecimal totalCost) {

                BigDecimal safeRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
                BigDecimal safeCost = totalCost != null ? totalCost : BigDecimal.ZERO;
                BigDecimal profit = safeRevenue.subtract(safeCost);

                return new DriverKpiDTO(
                        driverName,
                        totalKm,
                        fuelCost,
                        tripsCompleted,
                        safeRevenue,
                        safeCost,
                        profit
                );
        }

        // Helper method to calculate efficiency score (0-100)
        private static BigDecimal calculateEfficiencyScore(BigDecimal totalKm, BigDecimal fuelCost) {
                if (totalKm == null || totalKm.compareTo(BigDecimal.ZERO) <= 0 ||
                        fuelCost == null || fuelCost.compareTo(BigDecimal.ZERO) <= 0) {
                        return BigDecimal.ZERO;
                }

                try {
                        // Efficiency = km per currency unit (higher is better)
                        BigDecimal efficiency = totalKm.divide(fuelCost, 2, RoundingMode.HALF_UP);

                        // Normalize to 0-100 scale (assuming 10 km/$ is excellent = 100 points)
                        BigDecimal score = efficiency.multiply(BigDecimal.TEN);

                        if (score.compareTo(BigDecimal.valueOf(100)) > 0) score = BigDecimal.valueOf(100);
                        if (score.compareTo(BigDecimal.ZERO) < 0) score = BigDecimal.ZERO;

                        return score;
                } catch (Exception e) {
                        return BigDecimal.ZERO;
                }
        }

        // Additional calculated getters
        public BigDecimal getCostPerKm() {
                if (totalKm.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
                return totalCost.divide(totalKm, 4, RoundingMode.HALF_UP);
        }

        public BigDecimal getRevenuePerTrip() {
                if (tripsCompleted <= 0) return BigDecimal.ZERO;
                return totalRevenue.divide(BigDecimal.valueOf(tripsCompleted), 2, RoundingMode.HALF_UP);
        }

        public BigDecimal getProfitMargin() {
                if (totalRevenue.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
                return profit.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
        }
}
