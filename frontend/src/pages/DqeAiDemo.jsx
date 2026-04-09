import {
  ErrorPanel,
  LoadingPanel,
  PageHeader,
  ProjectSelectorPanel,
} from "../components/DashboardBits";
import { formatCurrency, formatNumber, formatPercent } from "../lib/capex";

function getRiskClass(level) {
  if (level === "FAIBLE") {
    return "ai-risk-green";
  }
  if (level === "MOYEN") {
    return "ai-risk-orange";
  }
  return "ai-risk-red";
}

function getScoreClass(score) {
  if ((score ?? 0) >= 80) {
    return "ai-score-green";
  }
  if ((score ?? 0) >= 55) {
    return "ai-score-orange";
  }
  return "ai-score-red";
}

/**
 * Cette page montre la version la plus demonstrative du moteur DQE.
 *
 * Le parcours utilisateur est simple :
 * - selection du projet actif
 * - upload du document
 * - analyse IA enrichie
 * - import des lignes propres dans SP2I
 *
 * On affiche volontairement :
 * - un score global
 * - les erreurs principales
 * - un tableau riche ligne par ligne
 */
export default function DqeAiDemo({ dashboard }) {
  const { dqeAiState, data, projectContext } = dashboard;
  const analysis = dqeAiState.result;
  const currencyCode = projectContext.activeCurrencyCode;

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Demo AI"
        title="Lecture intelligente de DQE"
        description="Charge ton PDF DQE, laisse le moteur SP2I estimer, classifier et recommander la meilleure strategie local, import ou mix."
      />

      <section className="top-grid">
        <ProjectSelectorPanel projectContext={projectContext} />

        <section className="panel import-panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Document source</p>
              <h3>Analyser sans importer</h3>
            </div>
          </div>

          <div className="import-form">
            <label className="filter-field">
              <span>Fichier DQE</span>
              <input
                className="file-input"
                type="file"
                accept=".pdf,.png,.jpg,.jpeg,.webp,.xlsx,.xls"
                onChange={(event) => dqeAiState.setFile(event.target.files?.[0] ?? null)}
              />
            </label>

            <button
              className="primary-button"
              type="button"
              onClick={dqeAiState.handleAnalyze}
              disabled={dqeAiState.loading}
            >
              {dqeAiState.loading ? "Analyse AI en cours..." : "Lancer l'analyse AI"}
            </button>
          </div>

          {dqeAiState.file && (
            <p className="helper-text">Document selectionne : {dqeAiState.file.name}</p>
          )}

          <p className="helper-text">
            Le moteur enrichit les lignes avec prix local, prix FOB Chine, risque import et decision.
          </p>

          {dqeAiState.importMessage && <p className="success-text">{dqeAiState.importMessage}</p>}
          {dqeAiState.error && <p className="error-text">{dqeAiState.error}</p>}
        </section>
      </section>

      {dqeAiState.loading && (
        <LoadingPanel message="Extraction du DQE, enrichissement des lignes et calcul du score global..." />
      )}

      <ErrorPanel error={data.error} />

      {analysis && (
        <>
          <section className="metrics-grid">
            <article className={`metric-card accent-cyan ${getScoreClass(analysis.scoreGlobal)}`}>
              <div>
                <p>Score global</p>
                <small>Confiance moyenne du moteur AI</small>
              </div>
              <div className="metric-main">
                <strong>{formatNumber(analysis.scoreGlobal)}</strong>
                <span className="metric-detail">{formatPercent((analysis.scoreGlobal ?? 0) / 100)}</span>
              </div>
            </article>

            <article className="metric-card accent-green">
              <div>
                <p>Lignes enrichies</p>
                <small>Lignes exploitables detectees</small>
              </div>
              <div className="metric-main">
                <strong>{formatNumber(analysis.lignesAnalysees)}</strong>
              </div>
            </article>

            <article className="metric-card accent-orange">
              <div>
                <p>Points d'attention</p>
                <small>Lignes avec alertes metier</small>
              </div>
              <div className="metric-main">
                <strong>{formatNumber(analysis.lignesAvecAlerte)}</strong>
              </div>
            </article>

            <article className="metric-card accent-purple">
              <div>
                <p>Import propre possible</p>
                <small>Lignes convertibles en DQE projet</small>
              </div>
              <div className="metric-main">
                <strong>
                  {formatNumber(
                    analysis.lignes.filter(
                      (line) =>
                        line.designation &&
                        line.lot &&
                        line.famille &&
                        line.prixLocalEstime != null &&
                        (line.quantite ?? 0) > 0
                    ).length
                  )}
                </strong>
              </div>
            </article>
          </section>

          <section className="content-grid">
            <article className="panel">
              <div className="panel-heading">
                <div>
                  <p className="panel-label">Alertes</p>
                  <h3>Erreurs et points d'attention</h3>
                </div>
              </div>

              <div className="stack-list">
                {analysis.erreurs?.length ? (
                  analysis.erreurs.map((error, index) => (
                    <div className="stack-row stack-row-static" key={`${error}-${index}`}>
                      <span>{error}</span>
                      <strong>AI</strong>
                    </div>
                  ))
                ) : (
                  <div className="empty-state">Aucune erreur bloquante detectee.</div>
                )}
              </div>
            </article>

            <article className="panel side-panel">
              <p className="panel-label">Action</p>
              <h3>Importer donnees propres</h3>
              <div className="stack-list">
                <div className="stack-row stack-row-static">
                  <span>Projet cible</span>
                  <strong>
                    {projectContext.projects.find(
                      (project) => String(project.id) === projectContext.activeProjectId
                    )?.name || "Aucun"}
                  </strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Mode d'import</span>
                  <strong>Lignes nettoyees</strong>
                </div>
              </div>

              <div className="demo-import-block">
                <p className="helper-text">
                  Le bouton ci-dessous enregistre seulement les lignes assez propres pour entrer dans le projet.
                </p>
                <button
                  className="primary-button"
                  type="button"
                  onClick={dqeAiState.handleImportCleanData}
                  disabled={dqeAiState.importLoading}
                >
                  {dqeAiState.importLoading ? "Import des donnees propres..." : "Importer donnees propres"}
                </button>
              </div>
            </article>
          </section>

          <section className="panel table-panel">
            <div className="panel-heading">
              <div>
                <p className="panel-label">Lignes AI</p>
                <h3>Enrichissement intelligent du DQE</h3>
              </div>
              <span className="table-count">{analysis.lignes.length} ligne(s)</span>
            </div>

            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Designation</th>
                    <th>Lot</th>
                    <th>Famille</th>
                    <th>Quantite</th>
                    <th>Prix local</th>
                    <th>Prix import FOB</th>
                    <th>Fournisseur</th>
                    <th>Risque</th>
                    <th>Decision</th>
                    <th>Score</th>
                    <th>Alertes</th>
                  </tr>
                </thead>
                <tbody>
                  {analysis.lignes.map((line, index) => (
                    <tr key={`${line.designation}-${index}`}>
                      <td>{line.designation || "-"}</td>
                      <td>{line.lot || "-"}</td>
                      <td>{line.famille || "-"}</td>
                      <td>{formatNumber(line.quantite)}</td>
                      <td>{line.prixLocalEstime == null ? "-" : formatCurrency(line.prixLocalEstime, currencyCode)}</td>
                      <td>{line.prixImportEstime == null ? "-" : formatCurrency(line.prixImportEstime, currencyCode)}</td>
                      <td>{line.fournisseurSuggestion || "-"}</td>
                      <td>
                        <span className={`decision-badge ${getRiskClass(line.niveauRisque)}`}>
                          {line.niveauRisque || "-"}
                        </span>
                      </td>
                      <td>
                        <span className={`decision-badge decision-${String(line.decision || "mix").toLowerCase()}`}>
                          {line.decision || "-"}
                        </span>
                      </td>
                      <td>
                        <span className={`decision-badge ${getScoreClass(line.scoreConfiance)}`}>
                          {formatNumber(line.scoreConfiance)}
                        </span>
                      </td>
                      <td>{line.alertes?.join(", ") || "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
