package com.pcagrade.order.util;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility class for standardizing the calculation of order durations
 *
 * JOB RULE: Duration = 3 × number of cards
 * - Each card requires 3 units of certification time
 * - Minimum duration: 3 minutes (minimum 1 card)
 * - Default duration if data is missing: 60 minutes
 */
@Component
public class DureeCalculator {

    /**
     * Constant: certification time per card (in minutes)
     */
    public static final int TEMPS_CERTIFICATION_PAR_CARTE = 3;

    /**
     * Default duration if calculation is impossible (in minutes)
     */
    public static final int DUREE_DEFAUT_MINUTES = 60;

    /**
     * Minimum order duration (in minutes)
     */
    public static final int DUREE_MINIMALE_MINUTES = 3; // 1 minimum card

    /**
     * Calculates the duration of an order based on the number of cards
     *
     * @param nombreCartes Number of cards in the order
     * @return Duration in minutes (3 × number of cards)
     */
    public static int calculerDureeMinutes(int nombreCartes) {
        if (nombreCartes <= 0) {
            return DUREE_MINIMALE_MINUTES; // 1 minimum card
        }
        return nombreCartes * TEMPS_CERTIFICATION_PAR_CARTE;
    }

    /**
     * Calculates the duration of an order based on the database data
     *
     * @param commandeData Map containing the order data
     * @return Duration in minutes calculated according to business rules
     */
    public static int calculerDureeDepuisCommande(Map<String, Object> commandeData) {
        try {
            // 1. Priority is given to the number of actual cards.
            Integer nombreCartesReelles = (Integer) commandeData.get("nombreCartesReelles");
            if (nombreCartesReelles != null && nombreCartesReelles > 0) {
                return calculerDureeMinutes(nombreCartesReelles);
            }

            // 2. Fallback on nombreCartes
            Integer nombreCartes = (Integer) commandeData.get("nombreCartes");
            if (nombreCartes != null && nombreCartes > 0) {
                return calculerDureeMinutes(nombreCartes);
            }

            // 3. Fallback on existing duration Minutes if consistent
            Integer dureeExistante = (Integer) commandeData.get("dureeMinutes");
            if (dureeExistante != null && dureeExistante >= DUREE_MINIMALE_MINUTES) {
                // Check if this duration is consistent with the card rules
                int cartesImpliquees = dureeExistante / TEMPS_CERTIFICATION_PAR_CARTE;
                if (dureeExistante == cartesImpliquees * TEMPS_CERTIFICATION_PAR_CARTE) {
                    return dureeExistante; // Consistent duration
                }
            }

            // 4. Last resort: default duration
            System.out.println(" No card data for order " +
                    commandeData.get("numeroCommande") + ", utilisation durée par défaut");
            return DUREE_DEFAUT_MINUTES;

        } catch (Exception e) {
            System.err.println(" Order duration calculation error " +
                    commandeData.get("numeroCommande") + ": " + e.getMessage());
            return DUREE_DEFAUT_MINUTES;
        }
    }

    /**
     * Converts a duration in minutes to a readable format
     *
     * @param dureeMinutes Duration in minutes
     * @return Format "Xh Ymin" ou "Ymin"
     */
    public static String formaterDuree(int dureeMinutes) {
        if (dureeMinutes < 60) {
            return dureeMinutes + "min";
        }

        int heures = dureeMinutes / 60;
        int minutes = dureeMinutes % 60;

        if (minutes == 0) {
            return heures + "h";
        } else {
            return heures + "h " + minutes + "min";
        }
    }

    /**
     * Calculate the theoretical number of cards based on the duration
     * Useful for reverse validation
     *
     * @param dureeMinutes Duration in minutes
     * @return Theoretical number of cards
     */
    public static int calculerNombreCartesTheorique(int dureeMinutes) {
        return Math.max(1, dureeMinutes / TEMPS_CERTIFICATION_PAR_CARTE);
    }

    /**
     * Verifies the consistency between the number of cards and the duration
     *
     * @param nombreCartes Number of cards declared
     * @param dureeMinutes Declared duration
     * @return true if consistent, false otherwise
     */
    public static boolean validerCoherence(int nombreCartes, int dureeMinutes) {
        int dureeAttendue = calculerDureeMinutes(nombreCartes);
        return dureeAttendue == dureeMinutes;
    }

    /**
     * Generates a duration calculation report for debugging
     *
     * @param commandeData Order data
     * @return Detailed calculation report
     */
    public static String genererRapportCalcul(Map<String, Object> commandeData) {
        StringBuilder rapport = new StringBuilder();
        rapport.append(" Order duration calculation ").append(commandeData.get("numeroCommande")).append(":\n");

        Integer nombreCartesReelles = (Integer) commandeData.get("nombreCartesReelles");
        Integer nombreCartes = (Integer) commandeData.get("nombreCartes");
        Integer dureeExistante = (Integer) commandeData.get("dureeMinutes");

        rapport.append("- Cartes réelles: ").append(nombreCartesReelles).append("\n");
        rapport.append("- Cartes déclarées: ").append(nombreCartes).append("\n");
        rapport.append("- Durée existante: ").append(dureeExistante).append(" min\n");

        int dureeCalculee = calculerDureeDepuisCommande(commandeData);
        rapport.append("- Durée calculée: ").append(dureeCalculee).append(" min (");
        rapport.append(formaterDuree(dureeCalculee)).append(")\n");
        rapport.append("- Cartes impliquées: ").append(calculerNombreCartesTheorique(dureeCalculee));

        return rapport.toString();
    }
}