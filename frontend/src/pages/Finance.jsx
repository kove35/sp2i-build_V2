// ===============================
// 1. IMPORTS
// ===============================
// On reutilise les composants dashboard deja existants
// pour produire une page Finance MVP rapidement.
import {
  ErrorPanel,
  FilterPanel,
  LoadingPanel,
  PageHeader,
  ProjectSelectorPanel,
} from "../components/DashboardBits";
import { formatCurrency, formatNumber } from "../lib/capex";

// ===============================
// 2. COMPOSANT PRINCIPAL
// ===============================
// Cette page est un MVP Finance.
// Elle transforme les donnees CAPEX deja disponibles en
// premiere lecture budget / decaissement / cashflow.
export default function Finance({ dashboard }) {
  const { data, filters, projectContext } = dashboard;
  const currencyCode = projectContext.activeCurrencyCode;
  const summary = data.summary;
  const coverage = data.coverageMetrics;

  const budgetCapex = summary?.capexBrut ?? 0;
  const optimisedCapex = summary?.capexOptimise ?? 0;
  const potentialSavings = summary?.economie ?? 0;
  const estimatedCommitted = budgetCapex * 0.35;
  const estimatedRemaining = Math.max(budgetCapex - estimatedCommitted, 0);
  const monthlyRunRate = budgetCapex / 12;

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Finance"
        title="Finance - MVP"
        description="Une premiere lecture budgetaire pour piloter le CAPEX, suivre les montants engages et preparer une future vision cashflow."
        backendHint={data.backendStatusMessage}
        backendHintTone={data.backendWakeInProgress ? "warning" : "info"}
      />

      <section className="section-copy-card">
        <p className="panel-label">Finance</p>
        <h3>Lecture budgetaire initiale</h3>
        <p className="helper-text">
          Cette version MVP donne une base exploitable pour lire le budget CAPEX, le montant engage,
          le reste a engager et la trajectoire de cashflow projet.
        </p>
      </section>

      <section className="top-grid">
        <ProjectSelectorPanel projectContext={projectContext} />
        <FilterPanel filters={filters} data={data} projectContext={projectContext} />
      </section>

      {(data.loading || data.catalogLoading) && <LoadingPanel message="Chargement du cockpit finance..." />}
      {!data.loading && <ErrorPanel error={data.error} backendHint={data.backendStatusMessage} />}

      {!data.loading && !data.error && (
        <>
          <section className="section-copy-card">
            <p className="panel-label">Finance</p>
            <h3>KPI financiers</h3>
            <p className="helper-text">
              Ces indicateurs servent de premiere couche de lecture pour la decision budgetaire et la
              priorisation des arbitrages CAPEX.
            </p>
          </section>

          <section className="metrics-grid">
            <article className="metric-card accent-cyan">
              <div>
                <p>Budget CAPEX</p>
                <small>Reference projet</small>
              </div>
              <div className="metric-main">
                <strong>{formatCurrency(budgetCapex, currencyCode)}</strong>
                <span className="metric-detail">{formatNumber(coverage.totalLines)} lignes consolidees</span>
              </div>
            </article>

            <article className="metric-card accent-green">
              <div>
                <p>CAPEX optimise</p>
                <small>Scenario meilleur choix</small>
              </div>
              <div className="metric-main">
                <strong>{formatCurrency(optimisedCapex, currencyCode)}</strong>
                <span className="metric-detail">Gain potentiel {formatCurrency(potentialSavings, currencyCode)}</span>
              </div>
            </article>

            <article className="metric-card accent-orange">
              <div>
                <p>Engage estime</p>
                <small>Lecture MVP</small>
              </div>
              <div className="metric-main">
                <strong>{formatCurrency(estimatedCommitted, currencyCode)}</strong>
                <span className="metric-detail">Base 35% du budget CAPEX</span>
              </div>
            </article>

            <article className="metric-card accent-blue">
              <div>
                <p>Run rate mensuel</p>
                <small>Projection simple</small>
              </div>
              <div className="metric-main">
                <strong>{formatCurrency(monthlyRunRate, currencyCode)}</strong>
                <span className="metric-detail">Projection sur 12 mois</span>
              </div>
            </article>
          </section>

          <section className="three-grid">
            <article className="panel insight-card">
              <p className="panel-label">Scenario</p>
              <h3>Capacite d'economie</h3>
              <strong>{formatCurrency(potentialSavings, currencyCode)}</strong>
              <p className="helper-text">
                Gain potentiel entre le brut et le scenario optimise.
              </p>
            </article>

            <article className="panel insight-card">
              <p className="panel-label">Scenario</p>
              <h3>Reste a engager</h3>
              <strong>{formatCurrency(estimatedRemaining, currencyCode)}</strong>
              <p className="helper-text">
                Estimation simple du budget restant a transformer en commandes ou travaux.
              </p>
            </article>

            <article className="panel insight-card">
              <p className="panel-label">Scenario</p>
              <h3>Articles a arbitrer</h3>
              <strong>{formatNumber(summary?.nbArticlesSansPrixChine ?? 0)}</strong>
              <p className="helper-text">
                Lignes encore insuffisamment valorisees pour fiabiliser la trajectoire finance.
              </p>
            </article>
          </section>

          <section className="section-copy-card">
            <p className="panel-label">Finance</p>
            <h3>Decaissements et cashflow</h3>
            <p className="helper-text">
              Ces blocs donnent une lecture budget vs engage, puis une projection simple du rythme
              de consommation du CAPEX.
            </p>
          </section>

          <section className="three-grid">
            <article className="panel">
              <div className="panel-heading">
                <div>
                  <p className="panel-label">Decaissements</p>
                  <h3>Lecture budget vs engage</h3>
                </div>
              </div>

              <div className="project-summary-list">
                <div className="project-summary-row">
                  <span>Budget CAPEX</span>
                  <strong>{formatCurrency(budgetCapex, currencyCode)}</strong>
                </div>
                <div className="project-summary-row">
                  <span>Engage estime</span>
                  <strong>{formatCurrency(estimatedCommitted, currencyCode)}</strong>
                </div>
                <div className="project-summary-row">
                  <span>Reste a engager</span>
                  <strong>{formatCurrency(estimatedRemaining, currencyCode)}</strong>
                </div>
              </div>
            </article>

            <article className="panel">
              <div className="panel-heading">
                <div>
                  <p className="panel-label">Cashflow</p>
                  <h3>Projection simple</h3>
                </div>
              </div>

              <div className="project-summary-list">
                <div className="project-summary-row">
                  <span>Mensuel moyen</span>
                  <strong>{formatCurrency(monthlyRunRate, currencyCode)}</strong>
                </div>
                <div className="project-summary-row">
                  <span>Sans prix Chine</span>
                  <strong>{formatCurrency(summary?.capexSansPrixChine ?? 0, currencyCode)}</strong>
                </div>
                <div className="project-summary-row">
                  <span>Articles a arbitrer</span>
                  <strong>{formatNumber(summary?.nbArticlesSansPrixChine ?? 0)}</strong>
                </div>
              </div>
            </article>

            <article className="panel">
              <div className="panel-heading">
                <div>
                  <p className="panel-label">MVP</p>
                  <h3>Feuille de route finance</h3>
                </div>
              </div>

              <div className="project-actions-grid">
                <div className="project-action-card">
                  <strong>Decaissements</strong>
                  <span>Suivi par periode, fournisseur et categorie CAPEX.</span>
                  <small>Etape suivante</small>
                </div>
                <div className="project-action-card">
                  <strong>Cashflow</strong>
                  <span>Courbe temporelle de sorties et besoins de tresorerie.</span>
                  <small>Etape suivante</small>
                </div>
                <div className="project-action-card">
                  <strong>Budget vs reel</strong>
                  <span>Comparaison du plan theorique avec les montants constates.</span>
                  <small>Etape suivante</small>
                </div>
              </div>
            </article>
          </section>
        </>
      )}
    </div>
  );
}
