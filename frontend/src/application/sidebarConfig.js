// ===============================
// 1. CONFIG NAVIGATION SAAS
// ===============================
// Cette configuration centralise la navigation metier.
// Pourquoi ?
// Parce qu'on veut pouvoir faire evoluer la sidebar
// sans melanger la structure produit dans le JSX.

export const SIDEBAR_SECTIONS = [
  {
    title: "Direction & Finance",
    icon: "DF",
    items: [
      {
        to: "/direction",
        label: "Dashboard global",
        description: "KPI, synthese CAPEX et indicateurs de decision",
      },
      {
        to: "/direction",
        label: "Pilotage CAPEX",
        description: "Arbitrages, masses budgetaires et recommandations",
      },
      {
        to: "/finance",
        label: "Finance / rentabilite",
        description: "Budget, cashflow MVP et lecture investissement",
      },
      {
        to: "/zones",
        label: "Indicateurs decisionnels",
        description: "Heatmap, lecture spatiale et zones prioritaires",
      },
    ],
  },
  {
    title: "Chantier & Planification",
    icon: "CP",
    items: [
      {
        to: "/planning",
        label: "Planning chantier",
        description: "Ordonnancement, rythme et jalons",
      },
      {
        to: "/planning",
        label: "Ordonnancement",
        description: "Sequence des postes et priorites terrain",
      },
      {
        to: "/chantier",
        label: "Suivi des taches",
        description: "Lecture terrain des lots et familles critiques",
      },
      {
        to: "/chantier",
        label: "Couts terrain",
        description: "Comparaison local / import et pilotage operationnel",
      },
    ],
  },
  {
    title: "Import & Sourcing",
    icon: "IS",
    items: [
      {
        to: "/import",
        label: "Analyse import Chine",
        description: "Lecture sourcing et audit du perimetre import",
      },
      {
        to: "/import",
        label: "Comparatif Local vs Import",
        description: "Arbitrage cout, gain et faisabilite",
      },
      {
        to: "/demo-ai",
        label: "Scoring IA DQE",
        description: "Analyse IA, scoring et priorisation",
      },
      {
        to: "/direction",
        label: "Optimisation sourcing",
        description: "Leviers de gain et recommandations CAPEX",
      },
    ],
  },
  {
    title: "Donnees & DQE",
    icon: "DQ",
    items: [
      {
        to: "/import",
        label: "Import DQE",
        description: "Chargement et structuration des donnees source",
      },
      {
        to: "/demo-classic",
        label: "DQE brut",
        description: "Lecture standard et controle de qualite",
      },
      {
        to: "/demo",
        label: "DQE enrichi (IA)",
        description: "Enrichissement, corrections et import des lignes",
      },
      {
        to: "/demo-ai",
        label: "Historique IA",
        description: "Comparaison des analyses et iterations",
      },
    ],
  },
];

export const SIDEBAR_UTILITY_LINKS = [
  {
    title: "Accueil",
    icon: "AC",
    items: [
      {
        to: "/",
        label: "Vue d'ensemble",
        description: "Accueil SaaS, connexion et projet demo",
      },
    ],
  },
  {
    title: "Projet",
    icon: "PR",
    items: [
      {
        to: "/project",
        label: "Vue projet",
        description: "Contexte, structure et cadre budgetaire",
      },
      {
        to: "/projects/create",
        label: "Structure & hypotheses",
        description: "Parametrage du projet actif",
      },
    ],
  },
  {
    title: "Administration",
    icon: "AD",
    items: [
      {
        to: "/admin",
        label: "Projet actif & session",
        description: "Administration simple du workspace",
      },
    ],
  },
];

export const MOBILE_NAV_ITEMS = [
  { to: "/direction", label: "Direction", icon: "DF" },
  { to: "/planning", label: "Planning", icon: "CP" },
  { to: "/import", label: "Import", icon: "IS" },
  { to: "/demo", label: "DQE", icon: "DQ" },
];
