// ===============================
// 1. IMPORTS
// ===============================
// useState sert a gerer les groupes replies.
// useLocation permet de garder la section active ouverte.
import { useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { SIDEBAR_SECTIONS } from "../application/sidebarConfig";
import NavIcon from "./NavIcon";

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
    Object.fromEntries(SIDEBAR_SECTIONS.map((group) => [group.id, true]))
  );

  // On affiche le projet actif dans la zone contextuelle.
  const activeProjectName =
    projectContext.projects.find((project) => String(project.id) === projectContext.activeProjectId)?.name ??
    "Aucun projet selectionne";
  const activeSection = SIDEBAR_SECTIONS.find((group) =>
    group.items.some((item) => item.to === location.pathname)
  );

  // Ce helper laisse automatiquement ouvert
  // le groupe qui contient la route active.
  function isGroupExpanded(group) {
    const hasActiveItem = group.items.some((item) => item.to === location.pathname);
    if (hasActiveItem) {
      return true;
    }

    return !collapsedGroups[group.id];
  }

  // Cette fonction replie ou rouvre une section metier.
  function toggleGroup(groupId) {
    setCollapsedGroups((current) => ({
      ...current,
      [groupId]: !current[groupId],
    }));
  }

  function handleNavigation() {
    onClose();
  }

  // Petit rendu reutilisable :
  // la config porte la structure, le JSX ne fait que l'afficher.
  function renderNavLinks(items, sectionTitle) {
    return items.map((item) => (
      <NavLink
        key={`${sectionTitle}-${item.to}-${item.label}`}
        to={item.to}
        end
        onClick={handleNavigation}
        className={({ isActive }) => `sidebar-link ${isActive ? "sidebar-link-active" : ""}`}
        title={item.description}
      >
        <span className="sidebar-link-indicator" aria-hidden="true" />
        <strong className="sidebar-link-title">{item.label}</strong>
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

      <aside
        id="app-sidebar"
        className={`sidebar ${isOpen ? "sidebar-open" : ""}`.trim()}
        aria-label="Navigation principale"
      >
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
          <div className="sidebar-brand-top">
            <span className="sidebar-brand-mark" aria-hidden="true">
              SB
            </span>
            <div className="sidebar-brand-copy">
              <p className="eyebrow">SP2I Build</p>
              <h1>Navigation</h1>
            </div>
          </div>
          <p className="sidebar-copy">
            5 sections maximum pour reduire la charge cognitive et accelerer la navigation.
          </p>
        </div>

        <section className="sidebar-context-card" aria-label="Contexte courant">
          <p className="sidebar-context-label">Section active</p>
          <strong>{activeSection?.title || "Accueil"}</strong>
          <span>{activeSection?.summary || "Vue d'ensemble du workspace."}</span>
        </section>

        <nav className="sidebar-nav sidebar-nav-groups">
          {SIDEBAR_SECTIONS.map((group) => {
            const isExpanded = isGroupExpanded(group);
            const groupPanelId = `sidebar-group-${group.id}`;

            return (
              <section className="sidebar-group" key={group.id}>
                <button
                  type="button"
                  className="sidebar-group-toggle"
                  onClick={() => toggleGroup(group.id)}
                  aria-expanded={isExpanded}
                  aria-controls={groupPanelId}
                >
                  <span className="sidebar-group-head">
                    <span className="sidebar-group-icon">
                      <NavIcon name={group.icon} className="sidebar-group-icon-svg" title={group.title} />
                    </span>
                    <span className="sidebar-group-title-wrap">
                      <span className="sidebar-group-title">{group.title}</span>
                      <span className="sidebar-group-summary">{group.summary}</span>
                    </span>
                  </span>
                  <span className="sidebar-group-meta">
                    <span className="sidebar-group-count">{group.items.length}</span>
                    <span className={`sidebar-group-caret ${isExpanded ? "sidebar-group-caret-open" : ""}`.trim()}>
                      +
                    </span>
                  </span>
                </button>

                <div
                  id={groupPanelId}
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

        <section className="sidebar-section sidebar-section-compact">
          <p className="eyebrow">Contexte</p>
          <div className="sidebar-card">
            <strong>{activeProjectName}</strong>
            <span className="sidebar-muted">{auth.isAuthenticated ? auth.authEmail : "Mode visiteur"}</span>

            {auth.isAuthenticated ? (
              <button className="ghost-button" type="button" onClick={auth.logout}>
                Se deconnecter
              </button>
            ) : (
              <NavLink to="/" className="ghost-button sidebar-inline-link" onClick={handleNavigation}>
                Ouvrir l'accueil
              </NavLink>
            )}
          </div>
        </section>
      </aside>
    </>
  );
}
