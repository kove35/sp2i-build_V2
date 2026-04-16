// ===============================
// 1. CONFIG NAVIGATION SAAS
// ===============================
// Cette configuration reste volontairement simple :
// - une seule source de verite pour la navigation
// - aucun changement de route
// - la possibilite d'ajouter plus tard des restrictions par role
//
// Les champs "roles" ne sont pas encore exploites dans le JSX,
// mais ils preparent une navigation admin / user sans rework complet.

export const SIDEBAR_SECTIONS = [
  {
    id: "direction-finance",
    title: "Direction & Finance",
    icon: "DF",
    summary: "Vue executive, arbitrages CAPEX et lecture budgetaire.",
    items: [
      {
        to: "/direction",
        label: "Vue executive",
        description: "KPI, synthese CAPEX et leviers d'arbitrage.",
        mobileLabel: "Direction",
        roles: ["user", "admin"],
      },
      {
        to: "/finance",
        label: "Finance CAPEX",
        description: "Budget, engagement et trajectoire financiere.",
        mobileLabel: "Finance",
        roles: ["user", "admin"],
      },
      {
        to: "/zones",
        label: "Analyse spatiale",
        description: "Heatmap batiment / niveau et zones prioritaires.",
        mobileLabel: "Zones",
        roles: ["user", "admin"],
      },
    ],
  },
  {
    id: "chantier-planification",
    title: "Chantier & Planification",
    icon: "CP",
    summary: "Execution terrain, ordonnancement et suivi des priorites.",
    items: [
      {
        to: "/planning",
        label: "Planning chantier",
        description: "Jalons, cadence et sequence des interventions.",
        mobileLabel: "Planning",
        roles: ["user", "admin"],
      },
      {
        to: "/chantier",
        label: "Suivi chantier",
        description: "Lots critiques, lecture terrain et pilotage operationnel.",
        mobileLabel: "Chantier",
        roles: ["user", "admin"],
      },
    ],
  },
  {
    id: "import-sourcing",
    title: "Import & Sourcing",
    icon: "IS",
    summary: "Chargement des sources, arbitrage local vs import et preparation sourcing.",
    items: [
      {
        to: "/import",
        label: "Import & sourcing",
        description: "Imports DQE, analyse sourcing et comparatif local / import.",
        mobileLabel: "Import",
        roles: ["user", "admin"],
      },
    ],
  },
  {
    id: "donnees-dqe",
    title: "Donnees & DQE",
    icon: "DQ",
    summary: "Controle qualite, enrichissement et lecture IA des DQE.",
    items: [
      {
        to: "/demo-classic",
        label: "DQE brut",
        description: "Lecture standard et controle initial du document.",
        mobileLabel: "Brut",
        roles: ["user", "admin"],
      },
      {
        to: "/demo",
        label: "DQE enrichi",
        description: "Structuration, corrections et import des lignes.",
        mobileLabel: "DQE",
        roles: ["user", "admin"],
      },
      {
        to: "/demo-ai",
        label: "Analyse IA DQE",
        description: "Scoring, priorisation et comparaison des iterations.",
        mobileLabel: "IA",
        roles: ["user", "admin"],
      },
    ],
  },
];

export const SIDEBAR_SUPPORT_SECTIONS = [
  {
    id: "workspace",
    title: "Workspace",
    icon: "WS",
    items: [
      {
        to: "/project",
        label: "Vue projet",
        description: "Contexte du projet actif et cadrage general.",
        roles: ["user", "admin"],
      },
      {
        to: "/projects/create",
        label: "Parametres projet",
        description: "Structure immobiliere, hypotheses et donnees de reference.",
        roles: ["user", "admin"],
      },
    ],
  },
  {
    id: "support",
    title: "Support",
    icon: "SP",
    items: [
      {
        to: "/",
        label: "Accueil",
        description: "Landing, connexion et point d'entree produit.",
        roles: ["guest", "user", "admin"],
      },
      {
        to: "/admin",
        label: "Administration",
        description: "Session active, controles de base et supervision simple.",
        roles: ["admin", "user"],
      },
    ],
  },
];

// La bottom navigation mobile ne garde que les entrees
// les plus frequentes pour limiter la charge cognitive.
export const MOBILE_NAV_ITEMS = [
  { to: "/direction", label: "Direction", icon: "DF" },
  { to: "/planning", label: "Planning", icon: "CP" },
  { to: "/import", label: "Import", icon: "IS" },
  { to: "/demo", label: "DQE", icon: "DQ" },
];
