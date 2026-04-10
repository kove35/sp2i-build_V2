// ===============================
// 1. IMPORTS
// ===============================
// On importe le hook de localisation pour savoir si la sidebar doit etre visible.
import { useLocation } from "react-router-dom";
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

  return (
    <div className="app-shell">
      <div className="background-orb orb-left" />
      <div className="background-orb orb-right" />

      <div className={`app-layout ${showSidebar ? "" : "app-layout-no-sidebar"}`.trim()}>
        {showSidebar ? (
          <Sidebar auth={dashboard.auth} projectContext={dashboard.projectContext} />
        ) : null}

        <main className={`app-content ${showSidebar ? "" : "app-content-full"}`.trim()}>
          {children}
        </main>
      </div>
    </div>
  );
}
