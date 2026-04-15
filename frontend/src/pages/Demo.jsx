import {
  ErrorPanel,
  LoadingPanel,
  PageHeader,
  ProjectSelectorPanel,
} from "../components/DashboardBits";
import { formatCurrency, formatNumber, formatPercent } from "../lib/capex";

/**
 * Cette page met en scene la capacite "wow" de SP2I :
 * analyser un DQE reel avant import.
 *
 * On montre ici :
 * - la lecture automatique du document
 * - le score de qualite global
 * - les alertes de donnees manquantes
 * - le detail ligne par ligne
 */
export default function Demo({ dashboard }) {
  const { dqeState, data, projectContext } = dashboard;
  const currencyCode = projectContext.activeCurrencyCode;
  const analysis = dqeState.analysisResult;

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Demo"
        title="Analyse intelligente de DQE"
        description="Une demonstration du moteur SP2I capable de lire un document DQE, d'en detecter les lignes, de les classifier et de scorer automatiquement leur qualite."
      />

      <section className="top-grid">
        <ProjectSelectorPanel projectContext={projectContext} />

        <section className="panel import-panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">IA Document</p>
              <h3>Analyser sans importer</h3>
            </div>
          </div>

          <div className="import-form">
            <label className="filter-field">
              <span>Document source</span>
              <input
                className="file-input"
                type="file"
                accept=".pdf,.png,.jpg,.jpeg,.webp,.xlsx,.xls,.csv,.json,.txt"
                onChange={(event) => dqeState.setDqeAnalysisFile(event.target.files?.[0] ?? null)}
              />
            </label>

            <div className="demo-actions">
              <button
                className="primary-button"
                type="button"
                onClick={dqeState.handleDqeAnalysis}
                disabled={dqeState.analysisLoading}
              >
                {dqeState.analysisLoading ? "Analyse en cours..." : "Lancer l'analyse DQE"}
              </button>

              <button
                className="ghost-button"
                type="button"
                onClick={dqeState.handleRealDemoAnalysis}
                disabled={dqeState.analysisLoading}
              >
                Analyser l'exemple reel
              </button>
            </div>
          </div>

          {dqeState.analysisFile && (
            <p className="helper-text">Document selectionne : {dqeState.analysisFile.name}</p>
          )}

          <p className="helper-text">
            La demo accepte PDF, image, Excel ou CSV structure pour montrer la puissance complete du moteur.
          </p>

          {dqeState.analysisImportMessage && <p className="success-text">{dqeState.analysisImportMessage}</p>}
          {dqeState.analysisError && <p className="error-text">{dqeState.analysisError}</p>}
        </section>
      </section>

      {dqeState.analysisLoading && <LoadingPanel message="Analyse du DQE et calcul du score global..." />}

      <ErrorPanel error={data.error} />

      {analysis && (
        <>
          <section className="metrics-grid">
            <article className="metric-card accent-cyan">
              <div>
                <p>Score global</p>
                <small>Qualite moyenne des lignes reconnues</small>
              </div>
              <div className="metric-main">
                <strong>{formatNumber(analysis.scoreGlobal)}</strong>
                <span className="metric-detail">
                  {formatPercent((analysis.scoreGlobal ?? 0) / 100)}
                </span>
              </div>
            </article>

            <article className="metric-card accent-green">
              <div>
                <p>Lignes analysees</p>
                <small>Nombre de lignes candidates comprises</small>
              </div>
              <div className="metric-main">
                <strong>{formatNumber(analysis.lignesAnalysees)}</strong>
              </div>
            </article>

            <article className="metric-card accent-orange">
              <div>
                <p>Lignes sans prix</p>
                <small>Postes a completer avant arbitrage</small>
              </div>
              <div className="metric-main">
                <strong>{formatNumber(analysis.lignesSansPrix)}</strong>
              </div>
            </article>

            <article className="metric-card accent-purple">
              <div>
                <p>Lignes non classees</p>
                <small>Classification metier a revoir</small>
              </div>
              <div className="metric-main">
                <strong>{formatNumber(analysis.lignesNonClassees)}</strong>
              </div>
            </article>
          </section>

          <section className="content-grid">
            <article className="panel">
              <div className="panel-heading">
                <div>
                  <p className="panel-label">Qualite</p>
                  <h3>Points d'attention</h3>
                </div>
              </div>

              <div className="stack-list">
                <div className="stack-row stack-row-static">
                  <span>Lignes sans quantite</span>
                  <strong>{formatNumber(analysis.lignesSansQuantite)}</strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Lignes sans prix</span>
                  <strong>{formatNumber(analysis.lignesSansPrix)}</strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Lignes non classees</span>
                  <strong>{formatNumber(analysis.lignesNonClassees)}</strong>
                </div>
              </div>
            </article>

            <article className="panel side-panel">
              <p className="panel-label">Capacites</p>
              <h3>Ce que la demo montre</h3>
              <div className="stack-list">
                <div className="stack-row stack-row-static">
                  <span>Extraction texte</span>
                  <strong>PDF / OCR</strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Classification</span>
                  <strong>Lot + Famille</strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Validation</span>
                  <strong>Quantite / Prix / Score</strong>
                </div>
              </div>

              <div className="demo-import-block">
                <p className="helper-text">
                  Si l'analyse te convient, tu peux transformer ce document en vraies lignes projet.
                </p>
                <button
                  className="primary-button"
                  type="button"
                  onClick={dqeState.handleImportAnalyzedFile}
                  disabled={dqeState.analysisImportLoading}
                >
                  {dqeState.analysisImportLoading
                    ? "Import dans le projet actif..."
                    : "Importer ces lignes dans le projet actif"}
                </button>
              </div>
            </article>
          </section>

          <section className="panel table-panel">
            <div className="panel-heading">
              <div>
                <p className="panel-label">Resultat</p>
                <h3>Lignes detectees dans le document</h3>
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
                    <th>PU</th>
                    <th>Total</th>
                    <th>Score</th>
                    <th>Statut</th>
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
                      <td>{line.prixUnitaire == null ? "-" : formatCurrency(line.prixUnitaire, currencyCode)}</td>
                      <td>{line.prixTotal == null ? "-" : formatCurrency(line.prixTotal, currencyCode)}</td>
                      <td>{formatNumber(line.score)}</td>
                      <td>
                        <span className={`decision-badge ${line.valide ? "decision-import" : "decision-missing"}`}>
                          {line.valide ? "VALIDE" : "A REVOIR"}
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
