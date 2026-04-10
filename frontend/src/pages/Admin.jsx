// ===============================
// 1. IMPORTS
// ===============================
// On reutilise les briques visuelles existantes pour garder
// un rendu coherent avec le reste de l'application.
import { PageHeader, ProjectSelectorPanel } from "../components/DashboardBits";

// ===============================
// 2. COMPOSANT PRINCIPAL
// ===============================
// Cette page est un MVP d'administration.
// Elle ne cree pas encore tous les modules, mais elle donne
// un vrai point d'entree pour :
// - projet actif
// - session utilisateur
// - parametres a venir
export default function Admin({ dashboard }) {
  const { auth, projectContext, data } = dashboard;

  const activeProject =
    projectContext.projects.find((project) => String(project.id) === projectContext.activeProjectId) ?? null;

  const adminBlocks = [
    {
      title: "Projet actif",
      value: activeProject?.name || "Aucun projet selectionne",
      detail: "Point d'ancrage de tous les dashboards et imports.",
    },
    {
      title: "Utilisateur",
      value: auth.isAuthenticated ? auth.authEmail : "Mode visiteur",
      detail: auth.isAuthenticated ? "Session authentifiee active." : "Connexion requise pour administrer.",
    },
    {
      title: "Etat backend",
      value: data.backendWakeInProgress ? "Reveil en cours" : "Disponible",
      detail: data.backendStatusMessage || "Backend pret pour les operations metier.",
    },
  ];

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Administration"
        title="Administration du workspace"
        description="Un espace simple pour piloter le projet actif, l'acces utilisateur et les futurs reglages d'administration."
        backendHint={data.backendStatusMessage}
        backendHintTone={data.backendWakeInProgress ? "warning" : "info"}
      />

      <section className="section-copy-card">
        <p className="panel-label">Administration</p>
        <h3>Pilotage de la plateforme</h3>
        <p className="helper-text">
          Cette page regroupe la gouvernance minimale du workspace : projet actif, session utilisateur,
          parametres visibles et chantiers d'administration a venir.
        </p>
      </section>

      <ProjectSelectorPanel projectContext={projectContext} />

      <section className="three-grid">
        {adminBlocks.map((block) => (
          <article className="panel insight-card" key={block.title}>
            <p className="panel-label">Administration</p>
            <h3>{block.title}</h3>
            <strong>{block.value}</strong>
            <p className="helper-text">{block.detail}</p>
          </article>
        ))}
      </section>

      <section className="section-copy-card">
        <p className="panel-label">Administration</p>
        <h3>Lecture projet et acces</h3>
        <p className="helper-text">
          Ce bloc permet de relire rapidement les donnees de reference et l'etat d'acces de
          l'utilisateur courant.
        </p>
      </section>

      <section className="three-grid">
        <article className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Projet</p>
              <h3>Parametres disponibles</h3>
            </div>
          </div>

          <div className="project-summary-list">
            <div className="project-summary-row">
              <span>Localisation</span>
              <strong>{activeProject?.location || "Non renseignee"}</strong>
            </div>
            <div className="project-summary-row">
              <span>Type</span>
              <strong>{activeProject?.type || "Non renseigne"}</strong>
            </div>
            <div className="project-summary-row">
              <span>Strategie</span>
              <strong>{activeProject?.strategyMode || "Non renseignee"}</strong>
            </div>
          </div>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Utilisateurs</p>
              <h3>Etat de session</h3>
            </div>
          </div>

          <div className="project-summary-list">
            <div className="project-summary-row">
              <span>Authentification</span>
              <strong>{auth.isAuthenticated ? "Active" : "Inactive"}</strong>
            </div>
            <div className="project-summary-row">
              <span>Email</span>
              <strong>{auth.authEmail || "Aucun"}</strong>
            </div>
            <div className="project-summary-row">
              <span>Utilisateur ID</span>
              <strong>{auth.authUserId || "Non disponible"}</strong>
            </div>
          </div>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Roadmap</p>
              <h3>Modules a venir</h3>
            </div>
          </div>

          <div className="project-actions-grid">
            <div className="project-action-card">
              <strong>Utilisateurs</strong>
              <span>Gestion des roles, droits et acces par projet.</span>
              <small>Prochaine iteration</small>
            </div>
            <div className="project-action-card">
              <strong>Parametres</strong>
              <span>Reglages du moteur CAPEX, sourcing et IA.</span>
              <small>Prochaine iteration</small>
            </div>
            <div className="project-action-card">
              <strong>Logs</strong>
              <span>Historique des imports, analyses et actions metier.</span>
              <small>Prochaine iteration</small>
            </div>
          </div>
        </article>
      </section>

      <section className="three-grid">
        <article className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Actions</p>
              <h3>Raccourcis administratifs</h3>
            </div>
          </div>

          <div className="project-actions-grid">
            <div className="project-action-card">
              <strong>Projet actif</strong>
              <span>Basculer vers la fiche projet pour ajuster structure et hypotheses.</span>
              <small>Workflow metier</small>
            </div>
            <div className="project-action-card">
              <strong>Dashboard</strong>
              <span>Verifier que les KPI et les imports sont bien rattaches au bon projet.</span>
              <small>Controle</small>
            </div>
          </div>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Vision produit</p>
              <h3>Priorites de construction</h3>
            </div>
          </div>

          <div className="project-summary-list">
            <div className="project-summary-row">
              <span>Gestion utilisateurs</span>
              <strong>A preparer</strong>
            </div>
            <div className="project-summary-row">
              <span>Parametres plateforme</span>
              <strong>A structurer</strong>
            </div>
            <div className="project-summary-row">
              <span>Journal d'activite</span>
              <strong>A connecter</strong>
            </div>
          </div>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Etat technique</p>
              <h3>Point de controle</h3>
            </div>
          </div>

          <div className="project-summary-list">
            <div className="project-summary-row">
              <span>Backend</span>
              <strong>{data.backendWakeInProgress ? "Reveil / retry" : "Operationnel"}</strong>
            </div>
            <div className="project-summary-row">
              <span>Projets disponibles</span>
              <strong>{projectContext.projects.length}</strong>
            </div>
            <div className="project-summary-row">
              <span>Session</span>
              <strong>{auth.isAuthenticated ? "Connectee" : "Visiteur"}</strong>
            </div>
          </div>
        </article>
      </section>
    </div>
  );
}
