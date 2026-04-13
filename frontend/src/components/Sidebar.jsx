// ===============================
// 1. IMPORTS
// ===============================
// useState sert a gerer les groupes replies.
// useLocation permet de garder la section active ouverte.
import { useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { SIDEBAR_SECTIONS, SIDEBAR_UTILITY_LINKS } from "../application/sidebarConfig";

// ===============================
// 2. COMPOSANT PRINCIPAL
// ===============================
// Cette sidebar est maintenant pilotee par configuration.
// Pourquoi ?
// - la navigation devient plus simple a faire evoluer
// - on structure l'outil par workflow metier
// - on garde les routes existantes intactes
export default function Sidebar({ auth, projectContext, isOpen = false, onClose = () => {} }) {
  const location = useLocation();

  // Par defaut, les sections metier sont repliees.
  // Cela rend la sidebar plus compacte et plus lisible.
  const [collapsedGroups, setCollapsedGroups] = useState(() =>
    Object.fromEntries(SIDEBAR_SECTIONS.map((group) => [group.title, true]))
  );

  // On affiche le projet actif dans la zone contextuelle.
  const activeProjectName =
    projectContext.projects.find((project) => String(project.id) === projectContext.activeProjectId)?.name ??
    "Aucun projet selectionne";

  // Ce helper laisse automatiquement ouvert
  // le groupe qui contient la route active.
  function isGroupExpanded(group) {
    const hasActiveItem = group.items.some((item) => item.to === location.pathname);
    if (hasActiveItem) {
      return true;
    }

    return !collapsedGroups[group.title];
  }

  // Cette fonction replie ou rouvre une section metier.
  function toggleGroup(groupTitle) {
    setCollapsedGroups((current) => ({
      ...current,
      [groupTitle]: !current[groupTitle],
    }));
  }

  // Petit rendu reutilisable pour ne pas dupliquer les liens.
  function renderNavLinks(items, sectionTitle) {
    return items.map((item) => (
      <NavLink
        key={`${sectionTitle}-${item.to}-${item.label}`}
        to={item.to}
        className={({ isActive }) => `sidebar-link ${isActive ? "sidebar-link-active" : ""}`}
      >
        <strong>{item.label}</strong>
        <span>{item.description}</span>
      </NavLink>
    ));
  }

  return (
    <>
      <button
        type="button"
        className={`sidebar-backdrop ${isOpen ? "sidebar-backdrop-visible" : ""}`.trim()}
        onClick={onClose}
        aria-label="Fermer la navigation"
      />

      <aside className={`sidebar ${isOpen ? "sidebar-open" : ""}`.trim()}>
        <div className="sidebar-mobile-head">
          <div className="sidebar-mobile-brand">
            <p className="eyebrow">SP2I Build</p>
            <strong>Navigation</strong>
          </div>

          <button className="ghost-button sidebar-close" type="button" onClick={onClose}>
            Fermer
          </button>
        </div>

        <div className="sidebar-brand">
          <p className="eyebrow">SP2I Build</p>
          <h1>BI Workspace</h1>
          <p className="sidebar-copy">
            Une navigation SaaS organisee par workflow metier, pour guider la decision du DQE brut
            jusqu'au pilotage CAPEX.
          </p>
        </div>

        <nav className="sidebar-nav sidebar-nav-groups">
          {SIDEBAR_SECTIONS.map((group) => {
            const isExpanded = isGroupExpanded(group);

            return (
              <section className="sidebar-group" key={group.title}>
                <button
                  type="button"
                  className="sidebar-group-toggle"
                  onClick={() => toggleGroup(group.title)}
                  aria-expanded={isExpanded}
                >
                  <span className="sidebar-group-head">
                    <span className="sidebar-group-icon">{group.icon}</span>
                    <span className="sidebar-group-title">{group.title}</span>
                  </span>
                  <span className="sidebar-group-caret">{isExpanded ? "-" : "+"}</span>
                </button>

                <div
                  className={`sidebar-group-links ${
                    isExpanded ? "" : "sidebar-group-links-collapsed"
                  }`.trim()}
                >
                  {renderNavLinks(group.items, group.title)}
                </div>
              </section>
            );
          })}
        </nav>

        <section className="sidebar-utility">
          {SIDEBAR_UTILITY_LINKS.map((group) => (
            <div className="sidebar-utility-block" key={group.title}>
              <div className="sidebar-utility-head">
                <span className="sidebar-group-icon">{group.icon}</span>
                <span className="sidebar-group-title">{group.title}</span>
              </div>

              <div className="sidebar-group-links">
                {renderNavLinks(group.items, group.title)}
              </div>
            </div>
          ))}
        </section>

        <section className="sidebar-section">
          <p className="eyebrow">Session</p>

          {auth.isAuthenticated ? (
            <div className="sidebar-card">
              <strong>{auth.authEmail}</strong>
              <span className="sidebar-muted">Session JWT active</span>

              <button className="ghost-button" type="button" onClick={auth.logout}>
                Se deconnecter
              </button>
            </div>
          ) : (
            <div className="sidebar-card">
              <strong>Mode visiteur</strong>
              <span className="sidebar-muted">
                Retournez a l'accueil pour vous connecter ou lancer le projet demo.
              </span>

              <NavLink to="/" className="ghost-button sidebar-inline-link">
                Ouvrir l'accueil
              </NavLink>
            </div>
          )}
        </section>

        <section className="sidebar-section">
          <p className="eyebrow">Projet actif</p>

          <div className="sidebar-card">
            <strong>{activeProjectName}</strong>
            <span className="sidebar-muted">
              Projet de reference pour les dashboards et les imports globaux.
            </span>

            <NavLink to="/projects/create" className="ghost-button sidebar-inline-link">
              Ouvrir la fiche projet
            </NavLink>
          </div>
        </section>
      </aside>
    </>
  );
}
