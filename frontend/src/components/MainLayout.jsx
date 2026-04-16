// ===============================
// 1. IMPORTS
// ===============================
// On importe React pour gerer l'ouverture du menu responsive.
import { useEffect, useState } from "react";
// On importe le hook de localisation pour savoir si la sidebar doit etre visible.
import { NavLink, useLocation } from "react-router-dom";
import { MOBILE_NAV_ITEMS, SIDEBAR_SECTIONS } from "../application/sidebarConfig";
import NavIcon from "./NavIcon";
import Sidebar from "./Sidebar";

// ===============================
// 2. COMPOSANT LAYOUT
// ===============================
// Ce layout unifie :
// - la structure generale de l'app
// - la sidebar SaaS
// - la zone de contenu
// On garde toutefois la page d'accueil sans sidebar pour conserver une vraie landing page.
export default function MainLayout({ dashboard, children }) {
  const location = useLocation();
  const isHomePage = location.pathname === "/";
  const showSidebar = !isHomePage;
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const activeSection =
    SIDEBAR_SECTIONS.find((section) => section.items.some((item) => item.to === location.pathname)) ?? null;

  // Quand l'utilisateur change de page,
  // on referme le menu mobile pour garder une navigation fluide.
  useEffect(() => {
    setSidebarOpen(false);
  }, [location.pathname]);

  return (
    <div className="app-shell">
      <div className="background-orb orb-left" />
      <div className="background-orb orb-right" />

      <div className={`app-layout ${showSidebar ? "" : "app-layout-no-sidebar"}`.trim()}>
        {showSidebar ? (
          <Sidebar
            auth={dashboard.auth}
            projectContext={dashboard.projectContext}
            isOpen={sidebarOpen}
            onClose={() => setSidebarOpen(false)}
          />
        ) : null}

        <main className={`app-content ${showSidebar ? "" : "app-content-full"}`.trim()}>
          {showSidebar ? (
            <div className="app-mobile-bar">
              <button
                className="app-mobile-menu"
                type="button"
                onClick={() => setSidebarOpen((current) => !current)}
                aria-expanded={sidebarOpen}
                aria-controls="app-sidebar"
              >
                {sidebarOpen ? "Fermer le menu" : "Ouvrir le menu"}
              </button>

              <div className="app-mobile-context">
                <span>{activeSection?.title || "Workspace SP2I"}</span>
                <strong>{activeSection?.summary || "Navigation metier"}</strong>
              </div>
            </div>
          ) : null}

          {children}
        </main>
      </div>

      {showSidebar ? (
        <nav className="app-bottom-nav">
          {MOBILE_NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end
              className={({ isActive }) => `app-bottom-link ${isActive ? "app-bottom-link-active" : ""}`}
            >
              <span className="app-bottom-icon">
                <NavIcon name={item.icon} className="app-bottom-icon-svg" title={item.label} />
              </span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      ) : null}
    </div>
  );
}
