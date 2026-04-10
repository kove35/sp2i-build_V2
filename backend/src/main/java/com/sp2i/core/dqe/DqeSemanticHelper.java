package com.sp2i.core.dqe;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ce composant regroupe les heuristiques metier de lecture DQE.
 *
 * Il sert de referentiel commun pour :
 * - la detection batiment / niveau
 * - la normalisation des lots contractuels
 * - la classification technique par famille
 * - le nettoyage des textes OCR / PDF
 */
@Component
public class DqeSemanticHelper {

    public static final String LOT_GROS_OEUVRE = "Gros oeuvre et demolition";
    public static final String LOT_ETANCHEITE = "Etancheite";
    public static final String LOT_REVETEMENTS_DURS = "Revetements durs";
    public static final String LOT_MENUISERIE_ALU = "Menuiserie aluminium et vitrerie";
    public static final String LOT_MENUISERIE_METALLIQUE = "Menuiserie metallique et ferronnerie";
    public static final String LOT_MENUISERIE_BOIS = "Menuiserie bois";
    public static final String LOT_ELECTRICITE = "Electricite";
    public static final String LOT_CLIMATISATION = "Climatisation";
    public static final String LOT_SECURITE = "Securite incendie et videosurveillance";
    public static final String LOT_PLOMBERIE = "Plomberie sanitaire";
    public static final String LOT_FAUX_PLAFOND = "Faux plafond et cloisons BA13";
    public static final String LOT_ASCENSEUR = "Ascenseur";
    public static final String LOT_ALUCOBOND = "Alucobond";
    public static final String LOT_PEINTURE = "Peinture";
    public static final String LOT_INCONNU = "DQE";

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

        if (containsAny(normalized, "gros", "demolition", "fondation", "elevation", "maconnerie", "beton")) {
            return LOT_GROS_OEUVRE;
        }
        if (normalized.contains("etanche")) {
            return LOT_ETANCHEITE;
        }
        if (normalized.contains("revet")) {
            return LOT_REVETEMENTS_DURS;
        }
        if (containsAny(normalized, "menuiserie aluminium", "alu", "vitrerie")) {
            return LOT_MENUISERIE_ALU;
        }
        if (containsAny(normalized, "menuiserie metall", "ferronnerie", "serrurerie")) {
            return LOT_MENUISERIE_METALLIQUE;
        }
        if (containsAny(normalized, "menuiserie bois", "bois")) {
            return LOT_MENUISERIE_BOIS;
        }
        if (normalized.contains("elect")) {
            return LOT_ELECTRICITE;
        }
        if (containsAny(normalized, "clim", "vrv", "hvac")) {
            return LOT_CLIMATISATION;
        }
        if (containsAny(normalized, "securite", "video", "incendie", "controle d'acces", "controle d acces")) {
            return LOT_SECURITE;
        }
        if (containsAny(normalized, "plomberie", "sanitaire")) {
            return LOT_PLOMBERIE;
        }
        if (containsAny(normalized, "faux plafond", "cloison", "ba13")) {
            return LOT_FAUX_PLAFOND;
        }
        if (normalized.contains("ascenseur")) {
            return LOT_ASCENSEUR;
        }
        if (normalized.contains("alucobond")) {
            return LOT_ALUCOBOND;
        }
        if (normalized.contains("peinture")) {
            return LOT_PEINTURE;
        }
        return normalize(sanitize(rawLotHeading));
    }

    public String normalizeBatimentHeading(String rawBuildingHeading) {
        String normalized = sanitize(rawBuildingHeading).toLowerCase(Locale.ROOT);

        if (normalized.contains("principal")) {
            return "Batiment Principal";
        }
        if (normalized.contains("annexe")) {
            return "Batiment Annexe";
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
        if (normalized.contains("parties communes chantier")) {
            return "PARTIES COMMUNES CHANTIER";
        }
        if (normalized.contains("parties communes")) {
            return "PARTIES COMMUNES";
        }
        if (normalized.contains("global") || normalized.contains("niveau chantier")) {
            return "GLOBAL";
        }
        return normalize(sanitize(rawLevelHeading));
    }

    public String inferLot(String designation) {
        String normalized = sanitize(designation).toLowerCase(Locale.ROOT);

        if (containsAny(normalized, "ascenseur")) {
            return LOT_ASCENSEUR;
        }
        if (containsAny(normalized, "alucobond", "ossature facade", "fixation facade", "panneau composite")) {
            return LOT_ALUCOBOND;
        }
        if (containsAny(normalized, "camera", "videosurveillance", "video surveillance", "nvr", "securite",
                "bloc autonome",
                "controle d'acces", "controle d acces", "detecteur", "declencheur", "extincteur",
                "alarme", "baes", "desenfumage", "ria", "incendie")) {
            return LOT_SECURITE;
        }
        if (containsAny(normalized, "clim", "split", "vrv", "refnet", "frigorifique", "condensat", "soufflage")) {
            return LOT_CLIMATISATION;
        }
        if (containsAny(normalized, "wc", "lavabo", "vasque", "evier", "receveur", "chauffe eau",
                "robinet", "collecteur", "surpression", "per ", "pvc d", "evacuation",
                "siphon", "tampon de fermeture", "compteur", "clapet anti-retour")) {
            return LOT_PLOMBERIE;
        }
        if (containsAny(normalized, "eclairage", "interrupteur", "prise ", "tableau", "cable",
                "gaine", "mosaique", "inverseur", "mise a la terre", "chemin de cable")) {
            return LOT_ELECTRICITE;
        }
        if (containsAny(normalized, "ba13", "faux plafond", "corniche", "isorelle", "laine de verre", "cloison")) {
            return LOT_FAUX_PLAFOND;
        }
        if (containsAny(normalized, "peinture", "impression", "sous-couche")) {
            return LOT_PEINTURE;
        }
        if (containsAny(normalized, "faience", "granite", "carrelage", "plinthe", "chape", "ragreage")) {
            return LOT_REVETEMENTS_DURS;
        }
        if (containsAny(normalized, "porte en alu", "baie vitree", "chassis", "vitrage", "facade vitree", "fenetre aluminium", "fenetre alu", "fenetre")) {
            return LOT_MENUISERIE_ALU;
        }
        if (containsAny(normalized, "porte metall", "grille", "garde corps metall", "serrurerie", "ferronnerie")) {
            return LOT_MENUISERIE_METALLIQUE;
        }
        if (containsAny(normalized, "placard", "etagere", "habillage bois", "porte bois", "ouvrant a la francaise")) {
            return LOT_MENUISERIE_BOIS;
        }
        if (containsAny(normalized, "etanche", "releve d'etancheite", "releve d etancheite", "pare-vapeur", "pare vapeur")) {
            return LOT_ETANCHEITE;
        }
        if (containsAny(normalized, "beton", "coffrage", "acier", "agglo", "maconnerie", "hourdis", "linteau",
                "poutre", "poteau", "voile", "longrine", "semelle", "dallage", "escalier ba", "demolition",
                "gravats", "decapage", "chantier", "mobilisation", "etudes techniques", "essais", "soubassement")) {
            return LOT_GROS_OEUVRE;
        }
        return LOT_INCONNU;
    }

    public String inferFamille(String designation, String lot) {
        String normalized = sanitize(designation).toLowerCase(Locale.ROOT);

        if (containsAny(normalized, "- acier", " acier")) {
            return "Acier";
        }
        if (containsAny(normalized, "- coffrage", " coffrage")) {
            return "Coffrage";
        }
        if (containsAny(normalized, "- beton", " beton ")) {
            return "Beton";
        }
        if (containsAny(normalized, "mobilisation", "bureau de chantier", "baraquement", "cloture provisoire", "abonnement")) {
            return "Installation de chantier";
        }
        if (containsAny(normalized, "materiel de chantier", "engin", "manutention")) {
            return "Materiel de chantier";
        }
        if (containsAny(normalized, "etudes techniques", "essais")) {
            return "Etudes techniques";
        }
        if (containsAny(normalized, "demolition")) {
            return "Demolition";
        }
        if (containsAny(normalized, "gravats")) {
            return "Evacuation gravats";
        }
        if (containsAny(normalized, "decapage")) {
            return "Decapage sols";
        }
        if (containsAny(normalized, "semelles isolees", "semelle isolee", "semelle")) {
            return "Semelles isolees";
        }
        if (normalized.contains("longrine")) {
            return "Longrines";
        }
        if (normalized.contains("dallage")) {
            return "Dallage beton";
        }
        if (containsAny(normalized, "poteaux en ba", "poteau en ba", "poteau")) {
            return "Poteaux BA";
        }
        if (containsAny(normalized, "poutres en ba", "poutre en ba", "poutre")) {
            return "Poutres BA";
        }
        if (containsAny(normalized, "voile en ba", "voile ba")) {
            return "Voiles BA";
        }
        if (normalized.contains("linteaux")) {
            return "Linteaux BA";
        }
        if (containsAny(normalized, "escalier ba", "escalier")) {
            return "Escaliers BA";
        }
        if (containsAny(normalized, "plancher", "hourdis")) {
            return "Plancher hourdis";
        }
        if (normalized.contains("beton")) {
            return "Beton";
        }
        if (normalized.contains("coffrage")) {
            return "Coffrage";
        }
        if (normalized.contains("acier")) {
            return "Acier";
        }
        if (containsAny(normalized, "agglo", "maconnerie")) {
            return "Maconnerie";
        }
        if (containsAny(normalized, "finition des tableaux", "tableaux")) {
            return "Finitions tableaux";
        }
        if (normalized.contains("enduits")) {
            return "Enduits";
        }
        if (normalized.contains("locaux humides")) {
            return "Etancheite locaux humides";
        }
        if (containsAny(normalized, "releve d'etancheite", "releve d etancheite")) {
            return "Releves d'etancheite";
        }
        if (containsAny(normalized, "etancheite", "revetement monocouche", "revetement bicouche", "protection lourde")) {
            return "Etancheite monocouche";
        }
        if (normalized.contains("faience")) {
            return "Carrelage mur";
        }
        if (containsAny(normalized, "granite tile", "carrelage sol", "sol -")) {
            return "Carrelage sol";
        }
        if (normalized.contains("plinthe")) {
            return "Plinthes";
        }
        if (containsAny(normalized, "baie vitree", "facade vitree")) {
            return "Facades vitrees";
        }
        if (normalized.contains("chassis")) {
            return "Chassis aluminium";
        }
        if (normalized.contains("vitrage")) {
            return "Vitrage";
        }
        if (containsAny(normalized, "porte en alu", "porte alu")) {
            return "Portes aluminium";
        }
        if (normalized.contains("porte metall")) {
            return "Portes metalliques";
        }
        if (normalized.contains("grille")) {
            return "Grilles";
        }
        if (normalized.contains("garde corps metall")) {
            return "Garde-corps metalliques";
        }
        if (containsAny(normalized, "serrurerie", "ferronnerie", "plateforme metallique", "escalier metallique")) {
            return "Serrurerie";
        }
        if (containsAny(normalized, "etagere", "placard")) {
            return "Placards bois";
        }
        if (normalized.contains("habillage bois")) {
            return "Habillages bois";
        }
        if (containsAny(normalized, "ouvrant a la francaise", "porte bois", "pb ", "pl ")) {
            return "Portes bois";
        }
        if (normalized.contains("tableau electrique")) {
            return "Tableaux electriques";
        }
        if (containsAny(normalized, "eclairage", "spot", "reglette", "applique", "panel led", "dalle led")) {
            return "Eclairage";
        }
        if (containsAny(normalized, "securite", "ase", "panneau de chantier")) {
            return "Securite / ASE";
        }
        if (containsAny(normalized, "interrupteur", "prise ", "detecteur de presence", "inverseur")) {
            return "Appareillage";
        }
        if (containsAny(normalized, "cable", "cablage")) {
            return containsAny(normalized, "reseau", "telephone", "tv") ? "Cablage informatique" : "Cables electriques";
        }
        if (containsAny(normalized, "gaine", "conduit", "soufflage", "reprise")) {
            return "Gaines / conduits";
        }
        if (containsAny(normalized, "clim", "split", "unite interieure", "unite exterieure", "vrv")) {
            return "Climatisation / splits";
        }
        if (containsAny(normalized, "frigorifique", "refnet", "condensat")) {
            return "Reseaux frigorifiques";
        }
        if (containsAny(normalized, "ventilation", "bouche d'extraction", "bouche d extraction", "vmc")) {
            return "Ventilation";
        }
        if (containsAny(normalized, "detecteur", "declencheur", "centrale de securite incendie", "alarme", "plaque d'identification")) {
            return "Detection incendie";
        }
        if (containsAny(normalized, "extincteur", "bac a sable", "ria")) {
            return "Extinction / RIA";
        }
        if (normalized.contains("camera")) {
            return "Cameras videosurveillance";
        }
        if (containsAny(normalized, "nvr", "serveur d'enregistrement", "serveur d enregistrement", "moniteur lcd")) {
            return "NVR / enregistreurs";
        }
        if (containsAny(normalized, "controle d'acces", "controle d acces")) {
            return "Controle d'acces";
        }
        if (containsAny(normalized, "wc", "lavabo", "vasque", "evier", "receveur")) {
            return "Appareils sanitaires";
        }
        if (normalized.contains("chauffe eau")) {
            return "Chauffe-eau";
        }
        if (containsAny(normalized, "canalisation evacuation", "evacuation pvc", "eaux usees", "eaux vannes", "eaux pluviales", "siphon", "fosses")) {
            return "Evacuation EU/EV";
        }
        if (containsAny(normalized, "alimentation per", "alimentation pvc", "collecteur", "vanne", "compteur", "clapet")) {
            return "Plomberie EF/EC";
        }
        if (containsAny(normalized, "surpression", "pompe", "vessie")) {
            return "Pompes de surpression";
        }
        if (containsAny(normalized, "cloison", "ba13")) {
            return "Cloisons BA13";
        }
        if (containsAny(normalized, "faux plafond", "isorelle", "staff lisse")) {
            return "Faux plafond BA13";
        }
        if (normalized.contains("corniche")) {
            return "Enduits de finition";
        }
        if (normalized.contains("ascenseur")) {
            return "Ascenseur";
        }
        if (containsAny(normalized, "alucobond", "panneau composite")) {
            return "Panneaux Alucobond";
        }
        if (normalized.contains("ossature facade")) {
            return "Ossature facade";
        }
        if (normalized.contains("fixation facade")) {
            return "Fixations facade";
        }
        if (containsAny(normalized, "murs - preparation", "preparation de surface")) {
            return "Sous-couche / impression";
        }
        if (containsAny(normalized, "peinture interieure", "peinture murs", "murs - peinture")) {
            return "Peinture murs";
        }
        if (containsAny(normalized, "plafonds staff", "peinture plafonds", "plafonds - peinture")) {
            return "Peinture plafonds";
        }

        return switch (lot) {
            case LOT_GROS_OEUVRE -> "Beton";
            case LOT_ETANCHEITE -> "Etancheite monocouche";
            case LOT_REVETEMENTS_DURS -> "Carrelage sol";
            case LOT_MENUISERIE_ALU -> "Chassis aluminium";
            case LOT_MENUISERIE_METALLIQUE -> "Serrurerie";
            case LOT_MENUISERIE_BOIS -> "Portes bois";
            case LOT_ELECTRICITE -> "Appareillage";
            case LOT_CLIMATISATION -> "Climatisation / splits";
            case LOT_SECURITE -> "Detection incendie";
            case LOT_PLOMBERIE -> "Plomberie EF/EC";
            case LOT_FAUX_PLAFOND -> "Faux plafond BA13";
            case LOT_ASCENSEUR -> "Ascenseur";
            case LOT_ALUCOBOND -> "Panneaux Alucobond";
            case LOT_PEINTURE -> "Peinture murs";
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
            return "Batiment Principal";
        }
        if (lower.contains("batiment annexe")) {
            return "Batiment Annexe";
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
        if (lower.contains("parties communes chantier")) {
            return "PARTIES COMMUNES CHANTIER";
        }
        if (lower.contains("parties communes")) {
            return "PARTIES COMMUNES";
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
                .replace("ÃƒÆ’Ã¢â‚¬Å¡", "Â")
                .replace("Ãƒâ€š", "Â")
                .replace("ÃƒÆ’Ã¢â‚¬Â°", "É")
                .replace("Ãƒâ€°", "É")
                .replace("ÃƒÆ’Ã‹â€ ", "È")
                .replace("ÃƒË†", "È")
                .replace("ÃƒÆ’Ã¢â€šÂ¬", "À")
                .replace("Ãƒâ‚¬", "À")
                .replace("ÃƒÆ’Ã¢â€žÂ¢", "Ù")
                .replace("Ãƒâ„¢", "Ù")
                .replace("ÃƒÆ’Ã‚Â¢", "â")
                .replace("ÃƒÂ¢", "â")
                .replace("ÃƒÆ’Ã‚Â©", "é")
                .replace("ÃƒÂ©", "é")
                .replace("ÃƒÆ’Ã‚Â¨", "è")
                .replace("ÃƒÂ¨", "è")
                .replace("ÃƒÆ’Ã‚Âª", "ê")
                .replace("ÃƒÂª", "ê")
                .replace("ÃƒÆ’Ã‚Â«", "ë")
                .replace("ÃƒÂ«", "ë")
                .replace("ÃƒÆ’Ã‚Â®", "î")
                .replace("ÃƒÂ®", "î")
                .replace("ÃƒÆ’Ã‚Â´", "ô")
                .replace("ÃƒÂ´", "ô")
                .replace("ÃƒÆ’Ã‚Â¹", "ù")
                .replace("ÃƒÂ¹", "ù")
                .replace("ÃƒÆ’Ã‚Â§", "ç")
                .replace("ÃƒÂ§", "ç")
                .replace("Ãƒâ€¦Ã¢â‚¬â„¢", "Œ")
                .replace("Ã…â€™", "Œ")
                .replace("Ãƒâ€¦Ã¢â‚¬Å“", "œ")
                .replace("Ã…â€œ", "œ");

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

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
