package com.sp2i.core.dqe;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ce composant regroupe les heuristiques simples de lecture metier
 * d'une ligne DQE.
 *
 * L'objectif est de centraliser ici les "petites regles" :
 * - detection du batiment
 * - detection du niveau
 * - classement lot / famille
 * - nettoyage des textes OCR / PDF
 */
@Component
public class DqeSemanticHelper {

    private static final Pattern BUILDING_PATTERN = Pattern.compile(
            "\\b(bat(?:iment)?\\s*[a-z0-9-]+)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LEVEL_PATTERN = Pattern.compile(
            "\\b(rdc|rez de chaussee|r\\+?\\d+|ss|sous-sol|niveau\\s*\\d+|etage\\s*\\d+|duplex\\s*\\d+|terrasse|fondations)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public String normalizeLotHeading(String rawLotHeading) {
        String normalized = sanitize(rawLotHeading).toLowerCase(Locale.ROOT);

        if (normalized.contains("gros") || normalized.contains("demolition")) {
            return "Gros oeuvre";
        }
        if (normalized.contains("etanche")) {
            return "Etancheite";
        }
        if (normalized.contains("revet") || normalized.contains("peinture")
                || normalized.contains("faux plafond") || normalized.contains("alucobond")) {
            return "Finitions";
        }
        if (normalized.contains("menuiserie")) {
            return "Menuiserie";
        }
        if (normalized.contains("elect")) {
            return "Electricite";
        }
        if (normalized.contains("clim")) {
            return "CVC";
        }
        if (normalized.contains("plomberie")) {
            return "Plomberie";
        }
        if (normalized.contains("ascenseur")) {
            return "Equipement";
        }
        if (normalized.contains("securite") || normalized.contains("video")) {
            return "Electricite";
        }
        return normalize(sanitize(rawLotHeading));
    }

    public String normalizeBatimentHeading(String rawBuildingHeading) {
        String normalized = sanitize(rawBuildingHeading).toLowerCase(Locale.ROOT);

        if (normalized.contains("principal")) {
            return "Bâtiment Principal";
        }
        if (normalized.contains("annexe")) {
            return "Bâtiment Annexe";
        }
        if (normalized.contains("site") || normalized.contains("chantier")) {
            return "Site";
        }
        return normalize(sanitize(rawBuildingHeading));
    }

    public String normalizeLevelHeading(String rawLevelHeading) {
        String normalized = sanitize(rawLevelHeading).toLowerCase(Locale.ROOT);

        if (normalized.contains("fondations")) {
            return "FONDATIONS";
        }
        if (normalized.contains("rez de chaussee") || normalized.contains("rdc")) {
            return "RDC";
        }
        if (normalized.contains("etage 1")) {
            return "ETAGE 1";
        }
        if (normalized.contains("etage 2")) {
            return "ETAGE 2";
        }
        if (normalized.contains("terrasse")) {
            return "TERRASSE";
        }
        if (normalized.contains("duplex 1")) {
            return "DUPLEX 1";
        }
        if (normalized.contains("duplex 2")) {
            return "DUPLEX 2";
        }
        if (normalized.contains("global")) {
            return "GLOBAL";
        }
        return normalize(sanitize(rawLevelHeading));
    }

    public String inferLot(String designation) {
        String normalized = sanitize(designation).toLowerCase(Locale.ROOT);

        if (normalized.contains("fenetre") || normalized.contains("porte") || normalized.contains("vitrage")) {
            return "Menuiserie";
        }
        if (normalized.contains("cable") || normalized.contains("eclairage") || normalized.contains("tableau")) {
            return "Electricite";
        }
        if (normalized.contains("tuyau") || normalized.contains("robinet") || normalized.contains("sanitaire")) {
            return "Plomberie";
        }
        if (normalized.contains("clim") || normalized.contains("ventilation") || normalized.contains("split")) {
            return "CVC";
        }
        if (normalized.contains("peinture") || normalized.contains("enduit") || normalized.contains("faux plafond")) {
            return "Finitions";
        }
        if (normalized.contains("beton") || normalized.contains("coffrage")
                || normalized.contains("acier") || normalized.contains("agglos")
                || normalized.contains("maconnerie") || normalized.contains("hourdis")) {
            return "Gros oeuvre";
        }
        return "DQE";
    }

    public String inferFamille(String designation, String lot) {
        String normalized = sanitize(designation).toLowerCase(Locale.ROOT);

        return switch (lot) {
            case "Menuiserie" -> normalized.contains("porte") ? "Portes" : "Fenetres";
            case "Electricite" -> normalized.contains("tableau") ? "Tableaux" : "Cables";
            case "Plomberie" -> normalized.contains("sanitaire") ? "Sanitaires" : "Tuyauterie";
            case "CVC" -> normalized.contains("ventilation") ? "Ventilation" : "Climatisation";
            case "Finitions" -> normalized.contains("faux plafond") ? "Faux plafond" : "Peinture";
            case "Gros oeuvre" -> "Structure";
            default -> "Autres";
        };
    }

    public String inferUnit(String designation) {
        String normalized = sanitize(designation).toLowerCase(Locale.ROOT);
        if (normalized.contains("m2")) {
            return "m2";
        }
        if (normalized.contains("m3")) {
            return "m3";
        }
        if (normalized.contains("ml")) {
            return "ml";
        }
        if (normalized.contains("kg")) {
            return "kg";
        }
        if (normalized.contains("ens")) {
            return "Ens";
        }
        return "U";
    }

    public String inferBatiment(String rawLine) {
        String normalizedLine = sanitize(rawLine);
        String lower = normalizedLine.toLowerCase(Locale.ROOT);
        if (lower.contains("batiment principal")) {
            return "Bâtiment Principal";
        }
        if (lower.contains("batiment annexe")) {
            return "Bâtiment Annexe";
        }
        if (lower.contains("site") || lower.contains("chantier")) {
            return "Site";
        }

        Matcher matcher = BUILDING_PATTERN.matcher(normalizedLine);
        if (matcher.find()) {
            return normalize(matcher.group(1));
        }
        return "BATIMENT_A_VERIFIER";
    }

    public String inferNiveau(String rawLine) {
        String lower = sanitize(rawLine).toLowerCase(Locale.ROOT);
        if (lower.contains("fondations")) {
            return "FONDATIONS";
        }
        if (lower.contains("rez de chaussee") || lower.contains("rdc")) {
            return "RDC";
        }
        if (lower.contains("etage 1")) {
            return "ETAGE 1";
        }
        if (lower.contains("etage 2")) {
            return "ETAGE 2";
        }
        if (lower.contains("terrasse")) {
            return "TERRASSE";
        }
        if (lower.contains("duplex 1")) {
            return "DUPLEX 1";
        }
        if (lower.contains("duplex 2")) {
            return "DUPLEX 2";
        }
        if (lower.contains("site") || lower.contains("chantier")) {
            return "GLOBAL";
        }

        Matcher matcher = LEVEL_PATTERN.matcher(sanitize(rawLine));
        if (matcher.find()) {
            return normalizeLevelHeading(matcher.group(1));
        }
        return "NIVEAU_A_VERIFIER";
    }

    /**
     * Corrige les caracteres degrages typiques des PDF/OCR.
     */
    public String sanitize(String value) {
        String safeValue = safe(value);
        if (safeValue.isBlank()) {
            return safeValue;
        }

        String repaired = safeValue
                .replace("Ãƒâ€š", "Â")
                .replace("Ã‚", "Â")
                .replace("Ãƒâ€°", "É")
                .replace("Ã‰", "É")
                .replace("ÃƒË†", "È")
                .replace("Ãˆ", "È")
                .replace("Ãƒâ‚¬", "À")
                .replace("Ã€", "À")
                .replace("Ãƒâ„¢", "Ù")
                .replace("Ã™", "Ù")
                .replace("ÃƒÂ¢", "â")
                .replace("Ã¢", "â")
                .replace("ÃƒÂ©", "é")
                .replace("Ã©", "é")
                .replace("ÃƒÂ¨", "è")
                .replace("Ã¨", "è")
                .replace("ÃƒÂª", "ê")
                .replace("Ãª", "ê")
                .replace("ÃƒÂ«", "ë")
                .replace("Ã«", "ë")
                .replace("ÃƒÂ®", "î")
                .replace("Ã®", "î")
                .replace("ÃƒÂ´", "ô")
                .replace("Ã´", "ô")
                .replace("ÃƒÂ¹", "ù")
                .replace("Ã¹", "ù")
                .replace("ÃƒÂ§", "ç")
                .replace("Ã§", "ç")
                .replace("Ã…â€™", "Œ")
                .replace("Å’", "Œ")
                .replace("Ã…â€œ", "œ")
                .replace("Å“", "œ");

        return Normalizer.normalize(repaired, Normalizer.Form.NFC)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String normalized = sanitize(value);
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }
}
