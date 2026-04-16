// ===============================
// 1. CONFIG SIDEBAR
// ===============================
// La navigation reste 100% pilotee par configuration.
// Cela permet de simplifier l'UX sans toucher aux routes React existantes.
//
// Regle produit :
// - 5 sections maximum
// - labels courts
// - pas de logique metier dupliquee dans le JSX

export const SIDEBAR_SECTIONS = [
  {
    id: "home",
    title: "Accueil",
    icon: "home",
    summary: "Vue d'ensemble, projet actif et raccourcis.",
    items: [
      {
        to: "/",
        label: "Vue",
        description: "Accueil produit",
        roles: ["guest", "user", "admin"],
      },
      {
        to: "/project",
        label: "Projet",
        description: "Projet actif",
        roles: ["user", "admin"],
      },
      {
        to: "/direction",
        label: "Rapide",
        description: "Acces rapide CAPEX",
        roles: ["user", "admin"],
      },
    ],
  },
  {
    id: "steering",
    title: "Pilotage",
    icon: "steering",
    summary: "CAPEX, finance, sourcing et analyse IA.",
    items: [
      {
        to: "/direction",
        label: "CAPEX",
        description: "Dashboard CAPEX",
        roles: ["user", "admin"],
      },
      {
        to: "/finance",
        label: "Finance",
        description: "Lecture budgetaire",
        roles: ["user", "admin"],
      },
      {
        to: "/import",
        label: "Sourcing",
        description: "Import & sourcing",
        roles: ["user", "admin"],
      },
      {
        to: "/demo-ai",
        label: "Analyse IA",
        description: "Scoring IA",
        roles: ["user", "admin"],
      },
    ],
  },
  {
    id: "execution",
    title: "Execution",
    icon: "execution",
    summary: "Planning, ordonnancement et suivi terrain.",
    items: [
      {
        to: "/planning",
        label: "Planning",
        description: "Planning chantier",
        roles: ["user", "admin"],
      },
      {
        to: "/chantier",
        label: "Terrain",
        description: "Suivi couts terrain",
        roles: ["user", "admin"],
      },
    ],
  },
  {
    id: "data",
    title: "Donnees",
    icon: "data",
    summary: "DQE, structure projet et hypotheses.",
    items: [
      {
        to: "/import",
        label: "Import DQE",
        description: "Import DQE",
        roles: ["user", "admin"],
      },
      {
        to: "/demo-classic",
        label: "Brut",
        description: "DQE brut",
        roles: ["user", "admin"],
      },
      {
        to: "/demo",
        label: "Enrichi",
        description: "DQE enrichi",
        roles: ["user", "admin"],
      },
      {
        to: "/projects/create",
        label: "Structure",
        description: "Structure & hypotheses",
        roles: ["user", "admin"],
      },
    ],
  },
  {
    id: "admin",
    title: "Administration",
    icon: "admin",
    summary: "Session, projet actif et parametres.",
    items: [
      {
        to: "/admin",
        label: "Session",
        description: "Session active",
        roles: ["user", "admin"],
      },
      {
        to: "/project",
        label: "Projet actif",
        description: "Fiche projet",
        roles: ["user", "admin"],
      },
      {
        to: "/projects/create",
        label: "Parametres",
        description: "Hypotheses projet",
        roles: ["user", "admin"],
      },
    ],
  },
];

// En mobile, on limite la bottom navigation a 5 points d'entree.
// Le reste reste accessible via le burger menu.
export const MOBILE_NAV_ITEMS = [
  { to: "/", label: "Accueil", icon: "home" },
  { to: "/direction", label: "Pilotage", icon: "steering" },
  { to: "/planning", label: "Execution", icon: "execution" },
  { to: "/demo", label: "Donnees", icon: "data" },
  { to: "/admin", label: "Admin", icon: "admin" },
];
