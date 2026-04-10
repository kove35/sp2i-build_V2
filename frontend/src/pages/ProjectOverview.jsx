// ===============================
// 1. IMPORTS
// ===============================
// On reutilise les composants UI deja presents dans le projet
// pour garder un rendu coherent avec le reste de l'application.
import { NavLink } from "react-router-dom";
import { MetricCard, PageHeader, ProjectSelectorPanel } from "../components/DashboardBits";
import { formatCurrency, formatNumber } from "../lib/capex";

// ===============================
// 2. COMPOSANT PRINCIPAL
// ===============================
// Cette page sert de hub "Projet".
// Elle resume :
// - la carte d'identite du projet
// - le cadre budgetaire
// - les hypotheses
// - les raccourcis vers les workflows
export default function ProjectOverview({ dashboard }) {
  const { data, projectContext } = dashboard;

  // On recupere le projet actuellement selectionne.
  const activeProject =
    projectContext.projects.find((project) => String(project.id) === projectContext.activeProjectId) ?? null;

  // On prend les KPI deja disponibles dans le dashboard global.
  const coverage = data.coverageMetrics;
  const currencyCode = activeProject?.currencyCode || "XAF";

  // Cette petite liste sert a afficher des tuiles d'action rapides.
  const quickActions = [
    {
      title: "Structure",
      description: "Mettre a jour la structure immobiliere et les hypotheses du projet.",
      to: "/projects/create",
      action: "Modifier",
    },
    {
      title: "Documents",
      description: "Importer un DQE, une base de travail ou relancer une analyse.",
      to: "/import",
      action: "Ouvrir",
    },
    {
      title: "DQE enrichi",
      description: "Corriger, enrichir et valider les lignes avec l'IA.",
      to: "/demo",
      action: "Analyser",
    },
    {
      title: "Pilotage",
      description: "Basculer vers la synthese de direction et les arbitrages CAPEX.",
      to: "/direction",
      action: "Piloter",
    },
  ];

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Projet"
        title="Vue projet"
        description="Un point d'entree unique pour comprendre le projet actif, son cadre budgetaire et les prochaines actions SaaS."
      />

      <ProjectSelectorPanel projectContext={projectContext} />

      <section className="metrics-grid">
        <MetricCard
          title="Budget projet"
          value={formatCurrency(activeProject?.budget ?? 0, currencyCode)}
          accent="cyan"
          subtitle="Cadre financier"
          detail={activeProject?.currencyCode || "XAF"}
        />
        <MetricCard
          title="Surface"
          value={formatNumber(activeProject?.surface ?? 0)}
          accent="green"
          subtitle="Surface declaree"
          detail="m2"
        />
        <MetricCard
          title="Lignes CAPEX"
          value={formatNumber(coverage.totalLines)}
          accent="orange"
          subtitle="Base projet"
          detail={`${formatNumber(coverage.lots)} lots | ${formatNumber(coverage.families)} familles`}
        />
        <MetricCard
          title="Structure"
          value={`${formatNumber(coverage.buildings)} / ${formatNumber(coverage.levels)}`}
          accent="blue"
          subtitle="Batiments / niveaux"
          detail="Zones couvertes"
        />
      </section>

      <section className="three-grid">
        <article className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Carte d'identite</p>
              <h3>Informations generales</h3>
            </div>
          </div>

          <div className="project-summary-list">
            <ProjectSummaryRow label="Nom du projet" value={activeProject?.name} />
            <ProjectSummaryRow label="Localisation" value={activeProject?.location} />
            <ProjectSummaryRow label="Type" value={activeProject?.type} />
            <ProjectSummaryRow label="Devise" value={activeProject?.currencyCode} />
            <ProjectSummaryRow label="Strategie" value={activeProject?.strategyMode} />
          </div>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Hypotheses</p>
              <h3>Cadre logistique et import</h3>
            </div>
          </div>

          <div className="project-summary-list">
            <ProjectSummaryRow label="Transport" value={formatPercentValue(activeProject?.transportRate)} />
            <ProjectSummaryRow label="Douane" value={formatPercentValue(activeProject?.douaneRate)} />
            <ProjectSummaryRow label="Port" value={formatPercentValue(activeProject?.portRate)} />
            <ProjectSummaryRow label="Local" value={formatPercentValue(activeProject?.localRate)} />
            <ProjectSummaryRow label="Marge" value={formatPercentValue(activeProject?.marginRate)} />
            <ProjectSummaryRow label="Risque" value={formatPercentValue(activeProject?.riskRate)} />
            <ProjectSummaryRow label="Seuil import" value={formatPercentValue(activeProject?.importThreshold)} />
          </div>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Workflow</p>
              <h3>Actions rapides</h3>
            </div>
          </div>

          <div className="project-actions-grid">
            {quickActions.map((entry) => (
              <NavLink className="project-action-card" to={entry.to} key={entry.title}>
                <strong>{entry.title}</strong>
                <span>{entry.description}</span>
                <small>{entry.action}</small>
              </NavLink>
            ))}
          </div>
        </article>
      </section>
    </div>
  );
}

// ===============================
// 3. SOUS-COMPOSANT
// ===============================
// Ce composant affiche une ligne label / valeur.
// On l'isole pour eviter de dupliquer le meme code partout.
function ProjectSummaryRow({ label, value }) {
  return (
    <div className="project-summary-row">
      <span>{label}</span>
      <strong>{value || "Non renseigne"}</strong>
    </div>
  );
}

// ===============================
// 4. UTILITAIRE
// ===============================
// Le backend renvoie parfois les taux sous forme decimale.
// On les convertit en pourcentage lisible.
function formatPercentValue(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return "";
  }

  return `${Math.round(Number(value) * 100)}%`;
}
