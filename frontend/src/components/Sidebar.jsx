// ===============================
// 1. IMPORTS
// ===============================
// useState sert a gerer les champs de login dans la sidebar.
// NavLink sert a garder une navigation react-router propre avec un etat actif visible.
import { useState } from "react";
import { NavLink } from "react-router-dom";

// ===============================
// 2. NAVIGATION METIER
// ===============================
// On regroupe la navigation par workflow utilisateur et non plus par type d'outil.
// Cela rend l'experience plus proche d'un vrai SaaS.
const NAV_GROUPS = [
  {
    title: "Accueil",
    items: [
      {
        to: "/",
        label: "Vue d'ensemble",
        description: "Landing, acces, demo et point d'entree",
      },
    ],
  },
  {
    title: "Projet",
    items: [
      {
        to: "/project",
        label: "Vue projet",
        description: "Vue d'ensemble, cadre budgetaire et actions rapides",
      },
      {
        to: "/projects/create",
        label: "Structure & hypotheses",
        description: "Projet actif, structure, hypotheses et cadre CAPEX",
      },
    ],
  },
  {
    title: "Donnees & DQE",
    items: [
      {
        to: "/import",
        label: "Import DQE",
        description: "Chargement, sourcing, audit et preparation",
      },
      {
        to: "/demo-classic",
        label: "DQE brut",
        description: "Lecture standard et controle de qualite",
      },
      {
        to: "/demo",
        label: "DQE enrichi (IA)",
        description: "Structuration, corrections et import des lignes",
      },
      {
        to: "/demo-ai",
        label: "Historique IA",
        description: "Lecture IA, scoring et comparaisons",
      },
    ],
  },
  {
    title: "Analyse & optimisation",
    items: [
      {
        to: "/demo-ai",
        label: "Scoring IA",
        description: "Lecture enrichie et suggestions intelligentes",
      },
      {
        to: "/import",
        label: "Audit Import Chine",
        description: "Arbitrage sourcing import / local",
      },
      {
        to: "/direction",
        label: "Optimisation CAPEX",
        description: "Comparaison de scenarios et recommandations",
      },
    ],
  },
  {
    title: "Decision & pilotage",
    items: [
      {
        to: "/direction",
        label: "KPI Direction",
        description: "Synthese CAPEX, familles, lots et arbitrages",
      },
      {
        to: "/zones",
        label: "Heatmap zones",
        description: "Lecture batiment / niveau et concentration CAPEX",
      },
    ],
  },
  {
    title: "Chantier",
    items: [
      {
        to: "/chantier",
        label: "Suivi couts",
        description: "Lecture terrain et decisions operationnelles",
      },
      {
        to: "/planning",
        label: "Planning",
        description: "Ordonnancement, avancement et rythme",
      },
    ],
  },
  {
    title: "Finance",
    items: [
      {
        to: "/finance",
        label: "Budget & cashflow",
        description: "Lecture MVP des budgets, engages et projections",
      },
    ],
  },
  {
    title: "Administration",
    items: [
      {
        to: "/admin",
        label: "Projet actif & session",
        description: "Projet, utilisateur, parametres et feuille de route",
      },
    ],
  },
];

// ===============================
// 3. COMPOSANT PRINCIPAL
// ===============================
export default function Sidebar({ auth, projectContext }) {
  // Champs de connexion inline dans la sidebar.
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // On calcule le nom du projet actif pour donner du contexte a l'utilisateur.
  const activeProjectName =
    projectContext.projects.find((project) => String(project.id) === projectContext.activeProjectId)?.name ??
    "Aucun projet selectionne";

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <p className="eyebrow">SP2I Build</p>
        <h1>BI Workspace</h1>
        <p className="sidebar-copy">
          Une navigation SaaS organisee par workflow metier, pour passer du DQE brut a la decision.
        </p>
      </div>

      <nav className="sidebar-nav sidebar-nav-groups">
        {NAV_GROUPS.map((group) => (
          <section className="sidebar-group" key={group.title}>
            <p className="sidebar-group-title">{group.title}</p>

            <div className="sidebar-group-links">
              {group.items.map((item) => (
                <NavLink
                  key={`${group.title}-${item.to}-${item.label}`}
                  to={item.to}
                  className={({ isActive }) => `sidebar-link ${isActive ? "sidebar-link-active" : ""}`}
                >
                  <strong>{item.label}</strong>
                  <span>{item.description}</span>
                </NavLink>
              ))}
            </div>
          </section>
        ))}
      </nav>

      <section className="sidebar-section">
        <p className="eyebrow">Authentification</p>

        {auth.isAuthenticated ? (
          <div className="sidebar-card">
            <strong>{auth.authEmail}</strong>
            <span className="sidebar-muted">Session JWT active</span>

            <button className="ghost-button" type="button" onClick={auth.logout}>
              Se deconnecter
            </button>
          </div>
        ) : (
          <div className="sidebar-card sidebar-form">
            <label className="filter-field">
              <span>Email</span>
              <input value={email} onChange={(event) => setEmail(event.target.value)} />
            </label>

            <label className="filter-field">
              <span>Mot de passe</span>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>

            <div className="sidebar-actions">
              <button
                className="primary-button"
                type="button"
                disabled={auth.authLoading}
                onClick={() => auth.login({ email, password })}
              >
                Login
              </button>

              <button
                className="ghost-button"
                type="button"
                disabled={auth.authLoading}
                onClick={() => auth.register({ email, password })}
              >
                Register
              </button>
            </div>

            {auth.authError ? <p className="error-text">{auth.authError}</p> : null}
          </div>
        )}
      </section>

      <section className="sidebar-section">
        <p className="eyebrow">Projet actif</p>

        <div className="sidebar-card">
          <strong>{activeProjectName}</strong>
          <span className="sidebar-muted">
            Projet de reference pour les dashboards et imports.
          </span>

          <NavLink to="/projects/create" className="ghost-button sidebar-inline-link">
            Ouvrir la fiche projet
          </NavLink>
        </div>
      </section>
    </aside>
  );
}
