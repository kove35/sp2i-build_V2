import { Route, Routes, useLocation } from "react-router-dom";
import Sidebar from "./components/Sidebar";
import { useCapexDashboardData } from "./hooks/useCapexDashboardData";
import Chantier from "./pages/Chantier";
import CreateProject from "./pages/CreateProject";
import Demo from "./pages/Demo";
import DqeAiDemo from "./pages/DqeAiDemo";
import DqeIntelligent from "./pages/DqeIntelligent";
import Direction from "./pages/Direction";
import HomePremium from "./pages/HomePremium";
import Import from "./pages/Import";
import Planning from "./pages/Planning";
import Zones from "./pages/Zones";

/**
 * Ce composant joue le role de coque principale de l'application.
 *
 * Il ne porte plus le contenu metier d'une seule page.
 * Son travail est maintenant de :
 * - afficher la sidebar
 * - partager les donnees du dashboard
 * - router vers la bonne vue metier
 */
function App() {
  const dashboard = useCapexDashboardData();
  const location = useLocation();
  const isHomePage = location.pathname === "/";
  const showSidebar = !isHomePage;

  return (
    <div className="app-shell">
      <div className="background-orb orb-left" />
      <div className="background-orb orb-right" />

      <div className={`app-layout ${showSidebar ? "" : "app-layout-no-sidebar"}`.trim()}>
        {showSidebar && <Sidebar auth={dashboard.auth} projectContext={dashboard.projectContext} />}

        <main className={`app-content ${showSidebar ? "" : "app-content-full"}`.trim()}>
          <Routes>
            <Route path="/" element={<HomePremium />} />
            <Route path="/demo" element={<DqeIntelligent dashboard={dashboard} />} />
            <Route path="/demo-ai" element={<DqeAiDemo dashboard={dashboard} />} />
            <Route path="/demo-classic" element={<Demo dashboard={dashboard} />} />
            <Route path="/direction" element={<Direction dashboard={dashboard} />} />
            <Route path="/import" element={<Import dashboard={dashboard} />} />
            <Route path="/chantier" element={<Chantier dashboard={dashboard} />} />
            <Route path="/zones" element={<Zones dashboard={dashboard} />} />
            <Route path="/planning" element={<Planning dashboard={dashboard} />} />
            <Route path="/projects/create" element={<CreateProject dashboard={dashboard} />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

export default App;
