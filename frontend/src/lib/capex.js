function normalizeApiBaseUrl(rawValue) {
  if (!rawValue) {
    return "/api";
  }

  if (rawValue.startsWith("http://") || rawValue.startsWith("https://") || rawValue.startsWith("/")) {
    return rawValue.replace(/\/$/, "");
  }

  return `https://${rawValue.replace(/\/$/, "")}`;
}

export const API_URL = normalizeApiBaseUrl(import.meta.env.VITE_API_URL);
export const API_BASE_URL = API_URL;
export const BACKEND_LABEL =
  import.meta.env.VITE_BACKEND_LABEL ||
  (API_URL === "/api" ? "proxy /api" : API_URL);

const LOT_LABELS = {
  "1": "Gros oeuvre",
  "2": "Etancheite",
  "3": "Revetements durs",
  "4": "Menuiserie aluminium et vitrerie",
  "5": "Menuiserie metallique et ferronnerie",
  "6": "Menuiserie bois",
  "7": "Electricite",
  "8": "Climatisation",
  "9": "Securite incendie et video surveillance",
  "10": "Plomberie sanitaire",
  "11": "Faux plafond et cloisons BA13",
  "12": "Ascenseur",
  "13": "Alucobond",
  "14": "Peinture",
};

/**
 * Construit une query string a partir des filtres actifs.
 * Si aucun filtre n'est renseigne, on retourne une chaine vide.
 */
export function buildQueryString(filters) {
  const params = new URLSearchParams();

  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      params.set(key, value);
    }
  });

  const queryString = params.toString();
  return queryString ? `?${queryString}` : "";
}

/**
 * Extrait une liste unique de valeurs pour remplir les select React.
 */
export function extractOptions(items, fieldName) {
  return [...new Set(items.map((item) => item[fieldName]).filter(Boolean))].sort((first, second) =>
    first.localeCompare(second)
  );
}

/**
 * Transforme la synthese backend en donnees exploitables par le graphique.
 */
export function buildChartData(summary, activeLot) {
  if (!summary?.parLot) {
    return [];
  }

  return Object.entries(summary.parLot).map(([name, lotSummary]) => ({
    name,
    label: normalizeLotLabel(name),
    capexBrut: lotSummary.capexBrut ?? 0,
    gainTotal: lotSummary.gainTotal ?? 0,
    taux: lotSummary.taux ?? 0,
    isActive: activeLot === name,
  }));
}

/**
 * Prepare une liste lisible pour la repartition par famille.
 */
export function buildFamilyEntries(summary, activeFamille) {
  if (!summary?.parFamille) {
    return [];
  }

  return Object.entries(summary.parFamille).map(([name, familySummary]) => ({
    name,
    label: normalizeFamilyLabel(name),
    capexBrut: familySummary.capexBrut ?? 0,
    gainTotal: familySummary.gainTotal ?? 0,
    taux: familySummary.taux ?? 0,
    isActive: activeFamille === name,
  }));
}

export function buildGroupedEntries(items, fieldName, activeValue = "") {
  if (!items?.length) {
    return [];
  }

  const groupedValues = new Map();

  items.forEach((item) => {
    const groupName = item?.[fieldName] || "Non renseigne";
    const currentGroup = groupedValues.get(groupName) || {
      name: groupName,
      capexBrut: 0,
      count: 0,
      quantite: 0,
    };

    currentGroup.capexBrut += (item?.coutLocal ?? 0) * (item?.quantite ?? 0);
    currentGroup.count += 1;
    currentGroup.quantite += item?.quantite ?? 0;
    groupedValues.set(groupName, currentGroup);
  });

  return [...groupedValues.values()]
    .sort((first, second) => second.capexBrut - first.capexBrut)
    .map((entry) => ({
      ...entry,
      label:
        fieldName === "lot"
          ? normalizeLotLabel(entry.name)
          : fieldName === "famille"
            ? normalizeFamilyLabel(entry.name)
            : normalizeZoneLabel(entry.name),
      isActive: activeValue === entry.name,
    }));
}

export function buildCoverageMetrics(items) {
  if (!items?.length) {
    return {
      totalLines: 0,
      totalQuantity: 0,
      buildings: 0,
      levels: 0,
      lots: 0,
      families: 0,
    };
  }

  return {
    totalLines: items.length,
    totalQuantity: items.reduce((sum, item) => sum + (item?.quantite ?? 0), 0),
    buildings: new Set(items.map((item) => item?.batiment).filter(Boolean)).size,
    levels: new Set(items.map((item) => item?.niveau).filter(Boolean)).size,
    lots: new Set(items.map((item) => item?.lot).filter(Boolean)).size,
    families: new Set(items.map((item) => item?.famille).filter(Boolean)).size,
  };
}

export function buildBatimentNiveauHeatmap(items, activeBatiment = "", activeNiveau = "") {
  if (!items?.length) {
    return {
      batiments: [],
      niveaux: [],
      maxValue: 0,
      cells: [],
    };
  }

  const batiments = [...new Set(items.map((item) => item?.batiment).filter(Boolean))].sort((a, b) =>
    a.localeCompare(b)
  );
  const niveaux = [...new Set(items.map((item) => item?.niveau).filter(Boolean))].sort((a, b) =>
    a.localeCompare(b)
  );

  const grouped = new Map();

  items.forEach((item) => {
    const batiment = item?.batiment || "Non renseigne";
    const niveau = item?.niveau || "Non renseigne";
    const key = `${batiment}::${niveau}`;
    const current = grouped.get(key) || {
      batiment,
      niveau,
      capexBrut: 0,
      count: 0,
      isActive: false,
    };

    current.capexBrut += (item?.coutLocal ?? 0) * (item?.quantite ?? 0);
    current.count += 1;
    current.isActive = activeBatiment === batiment && activeNiveau === niveau;
    grouped.set(key, current);
  });

  const cells = [...grouped.values()];
  const maxValue = Math.max(...cells.map((cell) => cell.capexBrut), 0);

  return {
    batiments: batiments.map((batiment) => ({
      name: batiment,
      label: normalizeZoneLabel(batiment),
    })),
    niveaux: niveaux.map((niveau) => ({
      name: niveau,
      label: normalizeZoneLabel(niveau),
    })),
    maxValue,
    cells: cells.map((cell) => ({
      ...cell,
      batimentLabel: normalizeZoneLabel(cell.batiment),
      niveauLabel: normalizeZoneLabel(cell.niveau),
    })),
  };
}

export function buildZoneEntries(items) {
  if (!items?.length) {
    return [];
  }

  const groupedZones = new Map();

  items.forEach((item) => {
    const batiment = item?.batiment || "Non renseigne";
    const niveau = item?.niveau || "Non renseigne";
    const key = `${batiment}::${niveau}`;
    const currentZone = groupedZones.get(key) || {
      key,
      batiment,
      niveau,
      batimentLabel: normalizeZoneLabel(batiment),
      niveauLabel: normalizeZoneLabel(niveau),
      capexBrut: 0,
      count: 0,
      lots: new Set(),
      families: new Set(),
    };

    currentZone.capexBrut += (item?.coutLocal ?? 0) * (item?.quantite ?? 0);
    currentZone.count += 1;

    if (item?.lot) {
      currentZone.lots.add(normalizeLotLabel(item.lot));
    }
    if (item?.famille) {
      currentZone.families.add(normalizeFamilyLabel(item.famille));
    }

    groupedZones.set(key, currentZone);
  });

  return [...groupedZones.values()]
    .sort((first, second) => second.capexBrut - first.capexBrut)
    .map((entry) => ({
      ...entry,
      lots: [...entry.lots],
      families: [...entry.families],
    }));
}

/**
 * Formate un montant selon la devise du projet actif.
 *
 * Par defaut, SP2I travaille ici en franc CFA (XAF),
 * car le projet de demonstration est base a Pointe-Noire.
 */
export function formatCurrency(value, currencyCode = "XAF") {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: currencyCode || "XAF",
    maximumFractionDigits: 0,
  }).format(value ?? 0);
}

export function formatPercent(value) {
  return new Intl.NumberFormat("fr-FR", {
    style: "percent",
    maximumFractionDigits: 1,
  }).format(value ?? 0);
}

export function formatNumber(value) {
  return new Intl.NumberFormat("fr-FR", {
    maximumFractionDigits: 2,
  }).format(value ?? 0);
}

/**
 * Convertit une etiquette de type "jour 15" en nombre exploitable.
 * Cela simplifie les petits calculs d'affichage dans la page planning.
 */
export function extractDayNumber(dayLabel) {
  if (!dayLabel) {
    return 0;
  }

  const match = String(dayLabel).match(/(\d+)/);
  return match ? Number(match[1]) : 0;
}

export function calculateGain(item) {
  if (item?.gain !== undefined && item?.gain !== null) {
    return item.gain;
  }
  const coutLocal = item?.coutLocal ?? 0;
  const coutImport = item?.coutImport ?? coutLocal;
  return coutLocal - coutImport;
}

export function getDecision(item) {
  if (item?.decision) {
    return item.decision;
  }
  const coutLocal = item?.coutLocal ?? 0;
  const coutImport = item?.coutImport ?? 0;
  return coutImport < coutLocal ? "IMPORT" : "LOCAL";
}

export function getDecisionVariant(item) {
  if (item?.coutImport == null) {
    return "missing";
  }

  const coutLocal = item?.coutLocal ?? 0;
  if (coutLocal === 0) {
    return "local";
  }

  const gapRatio = Math.abs(calculateGain(item)) / coutLocal;
  if (gapRatio <= 0.05) {
    return "mix";
  }

  return getDecision(item).toLowerCase();
}

export function getDecisionLabel(item) {
  const variant = getDecisionVariant(item);

  if (variant === "mix") {
    return "MIX";
  }

  if (variant === "missing") {
    return "SANS PRIX CHINE";
  }

  return getDecision(item);
}

export function buildTopEconomies(items) {
  return items
    .map((item) => ({
      ...item,
      gainTotal: calculateGain(item) * (item?.quantite ?? 0),
    }))
    .filter((item) => item.gainTotal > 0)
    .sort((first, second) => second.gainTotal - first.gainTotal)
    .slice(0, 5);
}

export function filterItemsByScenario(items, scenarioFilter = "") {
  if (!scenarioFilter) {
    return items;
  }

  const normalizedScenario = String(scenarioFilter).toUpperCase();

  return items.filter((item) => {
    const variant = getDecisionVariant(item).toUpperCase();

    if (normalizedScenario === "LOCAL") {
      return variant === "LOCAL";
    }

    if (normalizedScenario === "IMPORT") {
      return variant === "IMPORT";
    }

    if (normalizedScenario === "MIX") {
      return variant === "MIX";
    }

    return true;
  });
}

export function buildSummaryFromItems(items) {
  const safeItems = items ?? [];
  const emptySummary = {
    capexBrut: 0,
    capexOptimise: 0,
    economie: 0,
    gainTotal: 0,
    taux: 0,
    nbArticlesSansPrixChine: 0,
    capexSansPrixChine: 0,
    parLot: {},
    parFamille: {},
  };

  if (safeItems.length === 0) {
    return emptySummary;
  }

  const parLot = {};
  const parFamille = {};

  let capexBrut = 0;
  let capexOptimise = 0;
  let nbArticlesSansPrixChine = 0;
  let capexSansPrixChine = 0;

  safeItems.forEach((item) => {
    const quantite = item?.quantite ?? 0;
    const coutLocal = item?.coutLocal ?? item?.prixUnitaire ?? 0;
    const coutImport = item?.coutImport;
    const hasImportPrice = coutImport != null && Number.isFinite(coutImport);
    const localTotal = coutLocal * quantite;
    const importTotal = hasImportPrice ? coutImport * quantite : localTotal;
    const optimizedTotal = Math.min(localTotal, importTotal);
    const gainTotal = Math.max(localTotal - optimizedTotal, 0);
    const lot = item?.lot || "Non renseigne";
    const famille = item?.famille || "Non renseignee";

    capexBrut += localTotal;
    capexOptimise += optimizedTotal;

    if (!hasImportPrice) {
      nbArticlesSansPrixChine += 1;
      capexSansPrixChine += localTotal;
    }

    if (!parLot[lot]) {
      parLot[lot] = { capexBrut: 0, gainTotal: 0, taux: 0 };
    }
    if (!parFamille[famille]) {
      parFamille[famille] = { capexBrut: 0, gainTotal: 0, taux: 0 };
    }

    parLot[lot].capexBrut += localTotal;
    parLot[lot].gainTotal += gainTotal;
    parFamille[famille].capexBrut += localTotal;
    parFamille[famille].gainTotal += gainTotal;
  });

  Object.values(parLot).forEach((entry) => {
    entry.taux = entry.capexBrut > 0 ? entry.gainTotal / entry.capexBrut : 0;
  });

  Object.values(parFamille).forEach((entry) => {
    entry.taux = entry.capexBrut > 0 ? entry.gainTotal / entry.capexBrut : 0;
  });

  const economie = Math.max(capexBrut - capexOptimise, 0);

  return {
    capexBrut,
    capexOptimise,
    economie,
    gainTotal: economie,
    taux: capexBrut > 0 ? economie / capexBrut : 0,
    nbArticlesSansPrixChine,
    capexSansPrixChine,
    parLot,
    parFamille,
  };
}

/**
 * Petit indicateur d'avancement pour la page planning.
 * On calcule un taux de completude sur 4 dimensions metier.
 */
export function calculateCompletion(item) {
  const fields = [item?.lot, item?.famille, item?.batiment, item?.niveau];
  const completedFields = fields.filter(Boolean).length;
  return completedFields / fields.length;
}

export function normalizeLotLabel(value) {
  const rawValue = String(value || "").trim();

  if (!rawValue) {
    return "Non renseigne";
  }

  if (LOT_LABELS[rawValue]) {
    return LOT_LABELS[rawValue];
  }

  const normalizedValue = normalizeBiToken(rawValue);
  const withoutPrefix = normalizedValue.replace(/^[A-Z0-9]+\s*-\s*/i, "").trim();
  const lowerValue = withoutPrefix.toLowerCase();

  if (lowerValue.includes("gros") && lowerValue.includes("oeuvre")) return "Gros oeuvre";
  if (lowerValue.includes("etanche")) return "Etancheite";
  if (lowerValue.includes("revet")) return "Revetements durs";
  if (lowerValue.includes("menuiserie") && lowerValue.includes("alu")) return "Menuiserie aluminium et vitrerie";
  if (lowerValue.includes("menuiserie") && lowerValue.includes("metall")) return "Menuiserie metallique et ferronnerie";
  if (lowerValue.includes("menuiserie") && lowerValue.includes("bois")) return "Menuiserie bois";
  if (lowerValue.includes("elect")) return "Electricite";
  if (lowerValue.includes("clim")) return "Climatisation";
  if (lowerValue.includes("incend") || lowerValue.includes("video")) return "Securite incendie et video surveillance";
  if (lowerValue.includes("plomb")) return "Plomberie sanitaire";
  if (lowerValue.includes("faux plafond") || lowerValue.includes("ba13")) return "Faux plafond et cloisons BA13";
  if (lowerValue.includes("ascenseur")) return "Ascenseur";
  if (lowerValue.includes("aluco")) return "Alucobond";
  if (lowerValue.includes("peinture")) return "Peinture";

  return withoutPrefix || rawValue;
}

export function normalizeFamilyLabel(value) {
  const rawValue = String(value || "").trim();

  if (!rawValue) {
    return "Non renseignee";
  }

  const normalizedValue = normalizeBiToken(rawValue).replace(/^[A-Z0-9]+\s*-\s*/i, "").trim();
  const lowerValue = normalizedValue.toLowerCase();

  if (lowerValue.includes("vrv") && lowerValue.includes("ext")) return "VRV - Unites exterieures";
  if (lowerValue.includes("vrv") && lowerValue.includes("int")) return "VRV - Unites interieures";
  if (lowerValue.includes("vrv") && lowerValue.includes("autres")) return "VRV - Autres equipements";
  if (lowerValue.includes("eclairage") && lowerValue.includes("normal")) return "Eclairage normal";
  if (lowerValue.includes("balisage")) return "Eclairage de balisage";
  if (lowerValue.includes("commande")) return "Dispositifs de commande";
  if (lowerValue.includes("tableaux")) return "Tableaux electriques et accessoires";
  if (lowerValue.includes("installation")) return "Installation generale";
  if (lowerValue.includes("demolition")) return "Demolition";
  if (lowerValue.includes("fondation")) return "Fondations";
  if (lowerValue.includes("elevation")) return normalizedValue.replace(/\s+/g, " ");
  if (lowerValue.includes("menuiserie alu")) return normalizedValue.replace(/\s+/g, " ");
  if (lowerValue.includes("menuiserie bois")) return normalizedValue.replace(/\s+/g, " ");
  if (lowerValue.includes("menuiserie metall")) return "Menuiserie metallique";

  return normalizedValue || rawValue;
}

export function normalizeZoneLabel(value) {
  const rawValue = String(value || "").trim();

  if (!rawValue) {
    return "Non renseigne";
  }

  const normalizedValue = normalizeBiToken(rawValue);
  const lowerValue = normalizedValue.toLowerCase();

  if (lowerValue === "site") return "Site";
  if (lowerValue === "global") return "Global";
  if (lowerValue === "rdc") return "RDC";
  if (lowerValue === "etage 1") return "Etage 1";
  if (lowerValue === "etage 2") return "Etage 2";
  if (lowerValue === "duplex 1") return "Duplex 1";
  if (lowerValue === "duplex 2") return "Duplex 2";
  if (lowerValue === "terrasse") return "Terrasse";
  if (lowerValue === "parties communes") return "Parties communes";
  if (lowerValue === "parties communes chantier") return "Parties communes chantier";
  if (lowerValue === "niveau chantier") return "Niveau chantier";
  if (lowerValue.includes("batiment principal")) return "Batiment principal";
  if (lowerValue.includes("batiment annexe")) return "Batiment annexe";
  if (lowerValue.includes("facade")) return "Facade";

  return normalizedValue;
}

function normalizeBiToken(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, " ")
    .trim();
}
