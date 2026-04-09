import { useState } from "react";
import { NavLink } from "react-router-dom";

const NAV_ITEMS = [
  {
    to: "/",
    label: "Accueil",
    description: "Presentation du produit et porte d'entree",
  },
  {
    to: "/direction",
    label: "Direction",
    description: "KPI, synthese et vision executive",
  },
  {
    to: "/demo",
    label: "DQE intelligent",
    description: "Pipeline complet, scoring, enrichissement et import",
  },
  {
    to: "/demo-ai",
    label: "Demo AI",
    description: "Lecture AI enrichie et import propre",
  },
  {
    to: "/demo-classic",
    label: "Demo classique",
    description: "Analyse DQE standard et score de qualite",
  },
  {
    to: "/import",
    label: "Import",
    description: "Sourcing, matrices et chargement DQE",
  },
  {
    to: "/chantier",
    label: "Chantier",
    description: "Couts terrain et decisions operationnelles",
  },
  {
    to: "/zones",
    label: "Zones",
    description: "Lecture batiment, niveau et heatmap CAPEX",
  },
  {
    to: "/planning",
    label: "Planning",
    description: "Avancement, rythme et completude",
  },
  {
    to: "/projects/create",
    label: "Nouveau projet",
    description: "Creation complete avec structure et hypotheses",
  },
];

export default function Sidebar({ auth, projectContext }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <p className="eyebrow">SP2I Build</p>
        <h1>BI Workspace</h1>
        <p className="sidebar-copy">
          Une navigation claire pour explorer plusieurs angles du pilotage CAPEX.
        </p>
      </div>

      <nav className="sidebar-nav">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `sidebar-link ${isActive ? "sidebar-link-active" : ""}`}
          >
            <strong>{item.label}</strong>
            <span>{item.description}</span>
          </NavLink>
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
            {auth.authError && <p className="error-text">{auth.authError}</p>}
          </div>
        )}
      </section>

      <section className="sidebar-section">
        <p className="eyebrow">Projet actif</p>
        <div className="sidebar-card">
          <span className="sidebar-muted">
            {projectContext.projects.find((project) => String(project.id) === projectContext.activeProjectId)?.name ??
              "Aucun projet selectionne"}
          </span>
          <NavLink to="/projects/create" className="ghost-button sidebar-inline-link">
            Creer un projet complet
          </NavLink>
        </div>
      </section>
    </aside>
  );
}
