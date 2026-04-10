// ===============================
// 1. IMPORTS
// ===============================
// On garde exactement les pages existantes pour ne pas casser le projet.
// On ajoute seulement un layout principal pour mieux structurer l'application.
import { Route, Routes } from "react-router-dom";
import MainLayout from "./components/MainLayout";
import { useCapexDashboardData } from "./hooks/useCapexDashboardData";
import Admin from "./pages/Admin";
import Chantier from "./pages/Chantier";
import CreateProject from "./pages/CreateProject";
import Demo from "./pages/Demo";
import DqeAiDemo from "./pages/DqeAiDemo";
import DqeIntelligent from "./pages/DqeIntelligent";
import Direction from "./pages/Direction";
import Finance from "./pages/Finance";
import HomePremium from "./pages/HomePremium";
import Import from "./pages/Import";
import Planning from "./pages/Planning";
import ProjectOverview from "./pages/ProjectOverview";
import Zones from "./pages/Zones";

// ===============================
// 2. ROUTEUR PRINCIPAL
// ===============================
// Ce composant conserve les routes actuelles.
// La seule evolution est l'encapsulation dans un layout reutilisable.
function App() {
  const dashboard = useCapexDashboardData();

  return (
    <MainLayout dashboard={dashboard}>
      <Routes>
        <Route path="/" element={<HomePremium dashboard={dashboard} />} />
        <Route path="/demo" element={<DqeIntelligent dashboard={dashboard} />} />
        <Route path="/demo-ai" element={<DqeAiDemo dashboard={dashboard} />} />
        <Route path="/demo-classic" element={<Demo dashboard={dashboard} />} />
        <Route path="/project" element={<ProjectOverview dashboard={dashboard} />} />
        <Route path="/direction" element={<Direction dashboard={dashboard} />} />
        <Route path="/finance" element={<Finance dashboard={dashboard} />} />
        <Route path="/import" element={<Import dashboard={dashboard} />} />
        <Route path="/chantier" element={<Chantier dashboard={dashboard} />} />
        <Route path="/admin" element={<Admin dashboard={dashboard} />} />
        <Route path="/zones" element={<Zones dashboard={dashboard} />} />
        <Route path="/planning" element={<Planning dashboard={dashboard} />} />
        <Route path="/projects/create" element={<CreateProject dashboard={dashboard} />} />
      </Routes>
    </MainLayout>
  );
}

export default App;
