import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ErrorPanel,
  FilterSelect,
  LoadingPanel,
  PageHeader,
  ProjectSelectorPanel,
} from "../components/DashboardBits";
import { formatCurrency, formatNumber, formatPercent } from "../lib/capex";

function getDecisionClass(decision) {
  if (decision === "IMPORT") {
    return "decision-import";
  }
  if (decision === "LOCAL") {
    return "decision-local";
  }
  return "decision-mix";
}

function getRiskClass(risk) {
  if (risk === "FAIBLE") {
    return "ai-risk-green";
  }
  if (risk === "MOYEN") {
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
 * Cette page presente le pipeline intelligent complet DQE.
 */
export default function DqeIntelligent({ dashboard }) {
  const { dqeFullState, data, projectContext } = dashboard;
  const navigate = useNavigate();
  const result = dqeFullState.result;
  const currencyCode = projectContext.activeCurrencyCode;
  const [lotFilter, setLotFilter] = useState("");
  const [batimentFilter, setBatimentFilter] = useState("");
  const [niveauFilter, setNiveauFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  const filteredLines = useMemo(() => {
    if (!dqeFullState.editableLines?.length) {
      return [];
    }

    return dqeFullState.editableLines
      .map((line, sourceIndex) => ({ ...line, sourceIndex }))
      .filter((line) => {
      const matchesLot = !lotFilter || line.lot === lotFilter;
      const matchesBatiment = !batimentFilter || line.batiment === batimentFilter;
      const matchesNiveau = !niveauFilter || line.niveau === niveauFilter;
      const matchesStatus =
        !statusFilter
        || (statusFilter === "VALIDES" && line.valide)
        || (statusFilter === "ERREURS" && !line.valide)
        || (statusFilter === "SANS_BATIMENT" && line.erreurs?.includes("BATIMENT_NON_IDENTIFIE"))
        || (statusFilter === "SANS_NIVEAU" && line.erreurs?.includes("NIVEAU_NON_IDENTIFIE"))
        || (statusFilter === "NON_CLASSEES" && line.erreurs?.includes("NON_CLASSE"));

      return matchesLot && matchesBatiment && matchesNiveau && matchesStatus;
      });
  }, [dqeFullState.editableLines, lotFilter, batimentFilter, niveauFilter, statusFilter]);

  const lotOptions = useMemo(() => {
    if (!dqeFullState.editableLines?.length) {
      return [];
    }
    return [...new Set(dqeFullState.editableLines.map((line) => line.lot).filter(Boolean))];
  }, [dqeFullState.editableLines]);

  const batimentOptions = useMemo(() => {
    if (!dqeFullState.editableLines?.length) {
      return [];
    }
    return [...new Set(dqeFullState.editableLines.map((line) => line.batiment).filter(Boolean))];
  }, [dqeFullState.editableLines]);

  const niveauOptions = useMemo(() => {
    if (!dqeFullState.editableLines?.length) {
      return [];
    }
    return [...new Set(dqeFullState.editableLines.map((line) => line.niveau).filter(Boolean))];
  }, [dqeFullState.editableLines]);

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="DQE Intelligent"
        title="Pipeline intelligent DQE -> CAPEX"
        description="Charge un document brut, laisse SP2I l'extraire, le structurer, le scorer, l'enrichir, puis importe uniquement les lignes valides dans ton projet."
        backendHint={data.backendStatusMessage}
        backendHintTone={data.backendWakeInProgress ? "warning" : "info"}
      />

      <section className="top-grid">
        <ProjectSelectorPanel projectContext={projectContext} />

        <section className="panel import-panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Analyse</p>
              <h3>Document source</h3>
            </div>
          </div>

          <div className="import-form">
            <label className="filter-field">
              <span>Upload DQE</span>
              <input
                className="file-input"
                type="file"
                accept=".pdf,.png,.jpg,.jpeg,.webp,.xlsx,.xls,.csv,.json,.txt"
                onChange={(event) => dqeFullState.setFile(event.target.files?.[0] ?? null)}
              />
            </label>

            <button
              className="primary-button"
                type="button"
                onClick={dqeFullState.handleAnalyze}
                disabled={dqeFullState.loading}
              >
                {dqeFullState.loading ? "Analyse intelligente..." : "Analyser le DQE"}
              </button>

            <div className="structured-entry-block">
              <label className="filter-field">
                <span>Ou coller un tableau structure</span>
                <textarea
                  className="structured-textarea"
                  value={dqeFullState.structuredText}
                  onChange={(event) => dqeFullState.setStructuredText(event.target.value)}
                  placeholder={"Lot;Sous-lot;Bâtiment;Niveau;Désignation;Unité;Quantité;PU;Total\n1;A - Installation générale;Chantier;Niveau Chantier;Mobilisation;Ens;1;4755250;4755250"}
                  rows={8}
                />
              </label>

              <button
                className="ghost-button"
                type="button"
                onClick={dqeFullState.handleAnalyzeStructuredText}
                disabled={dqeFullState.loading}
              >
                {dqeFullState.loading ? "Analyse du tableau..." : "Analyser le tableau colle"}
              </button>

              <button
                className="primary-button"
                type="button"
                onClick={dqeFullState.handleDirectImportStructuredText}
                disabled={dqeFullState.importLoading}
              >
                {dqeFullState.importLoading ? "Import direct du tableau..." : "Importer directement le tableau colle"}
              </button>
            </div>

            <button
              className="secondary-button"
              type="button"
              onClick={dqeFullState.handleRealDemoAnalysis}
              disabled={dqeFullState.loading}
            >
              {dqeFullState.loading ? "Chargement de la demo..." : "Analyser l'exemple reel"}
            </button>
          </div>

          {dqeFullState.file && (
            <p className="helper-text">Document selectionne : {dqeFullState.file.name}</p>
          )}
          {!dqeFullState.file && dqeFullState.structuredText?.trim() && (
            <p className="helper-text">Source selectionnee : tableau structure colle dans la zone de saisie.</p>
          )}

          {dqeFullState.loading && (
            <div className="progress-block">
              <div className="progress-bar">
                <span style={{ width: `${dqeFullState.progress}%` }} />
              </div>
              <p className="helper-text">Progression : {dqeFullState.progress}%</p>
            </div>
          )}

          {dqeFullState.importMessage && <p className="success-text">{dqeFullState.importMessage}</p>}
          {dqeFullState.error && <p className="error-text">{dqeFullState.error}</p>}
        </section>
      </section>

      {dqeFullState.importMessage && (
        <section className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Dashboards</p>
              <h3>Explorer les donnees importees</h3>
            </div>
          </div>

          <p className="helper-text">
            Les lignes collees puis importees alimentent maintenant les dashboards du projet actif.
          </p>

          <div className="demo-actions dashboard-shortcuts">
            <button className="primary-button" type="button" onClick={() => navigate("/direction")}>
              Ouvrir Direction
            </button>
            <button className="ghost-button" type="button" onClick={() => navigate("/import")}>
              Ouvrir Import / DQE
            </button>
            <button className="ghost-button" type="button" onClick={() => navigate("/chantier")}>
              Ouvrir Chantier
            </button>
            <button className="ghost-button" type="button" onClick={() => navigate("/planning")}>
              Ouvrir Planning
            </button>
          </div>
        </section>
      )}

      {dqeFullState.loading && (
        <LoadingPanel message="Extraction, analyse IA, validation metier, scoring et enrichissement en cours..." />
      )}

      <ErrorPanel error={data.error} backendHint={data.backendStatusMessage} />

      {result && (
        <>
          <section className="metrics-grid">
            <article className={`metric-card accent-cyan ${getScoreClass(result.scoreGlobal)}`}>
              <div>
                <p>Score global</p>
                <small>Exploitabilite du DQE</small>
              </div>
              <div className="metric-main">
                <strong>{formatNumber(result.scoreGlobal)}</strong>
                <span className="metric-detail">{formatPercent((result.scoreGlobal ?? 0) / 100)}</span>
              </div>
            </article>

            <article className="metric-card accent-green">
              <div>
                <p>CAPEX total</p>
                <small>Scenario local / reference</small>
              </div>
              <div className="metric-main">
                <strong>{formatCurrency(result.capexTotal, currencyCode)}</strong>
              </div>
            </article>

            <article className="metric-card accent-orange">
              <div>
                <p>CAPEX optimise</p>
                <small>Scenario meilleur choix</small>
              </div>
              <div className="metric-main">
                <strong>{formatCurrency(result.capexOptimise, currencyCode)}</strong>
              </div>
            </article>

            <article className="metric-card accent-purple">
              <div>
                <p>Economie</p>
                <small>Gain potentiel du sourcing</small>
              </div>
              <div className="metric-main">
                <strong>{formatCurrency(result.economie, currencyCode)}</strong>
              </div>
            </article>
          </section>

          <section className="content-grid">
            <article className="panel">
              <div className="panel-heading">
                <div>
                  <p className="panel-label">Qualite</p>
                  <h3>Compteurs de validation</h3>
                </div>
              </div>

              <div className="stack-list">
                <div className="stack-row stack-row-static">
                  <span>Lignes valides</span>
                  <strong>{formatNumber(result.lignesValides)}</strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Lignes en erreur</span>
                  <strong>{formatNumber(result.lignesErreur)}</strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Lignes sans prix</span>
                  <strong>{formatNumber(result.lignesSansPrix)}</strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Lignes sans quantite</span>
                  <strong>{formatNumber(result.lignesSansQuantite)}</strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Lignes non classees</span>
                  <strong>{formatNumber(result.lignesNonClassees)}</strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Lignes sans batiment</span>
                  <strong>{formatNumber(result.lignesSansBatiment)}</strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Lignes sans niveau</span>
                  <strong>{formatNumber(result.lignesSansNiveau)}</strong>
                </div>
              </div>
            </article>

            <article className="panel side-panel">
              <p className="panel-label">Alertes</p>
              <h3>Points a corriger</h3>
              <div className="stack-list">
                {result.alertes?.length ? (
                  result.alertes.map((alert, index) => (
                    <div className="stack-row stack-row-static" key={`${alert}-${index}`}>
                      <span>{alert}</span>
                      <strong>DQE</strong>
                    </div>
                  ))
                ) : (
                  <div className="empty-state">Aucune alerte majeure detectee.</div>
                )}
              </div>

              <div className="demo-import-block">
                <p className="helper-text">
                  Les lignes valides peuvent maintenant etre importees dans le projet actif.
                </p>
                <button
                  className="primary-button"
                  type="button"
                  onClick={dqeFullState.handleImportValidLines}
                  disabled={dqeFullState.importLoading}
                >
                  {dqeFullState.importLoading ? "Import des lignes valides..." : "Importer lignes valides"}
                </button>
              </div>
            </article>
          </section>

          <section className="content-grid">
            <article className="panel">
              <div className="panel-heading">
                <div>
                  <p className="panel-label">Audit</p>
                  <h3>Blocs de coherence DQE</h3>
                </div>
              </div>

              {result.auditBlocs?.length ? (
                <div className="stack-list">
                  {result.auditBlocs.map((block, index) => (
                    <div className={`stack-row stack-row-static ${block.coherent ? "audit-row-ok" : "audit-row-ko"}`} key={`${block.batiment}-${block.niveau}-${index}`}>
                      <span>
                        {block.batiment || "Site"} | {block.niveau || "GLOBAL"} | {formatNumber(block.lignesDetectees)} ligne(s)
                      </span>
                      <strong>
                        {formatCurrency(block.sousTotalCalcule, currencyCode)} / {formatCurrency(block.sousTotalDocument, currencyCode)}
                      </strong>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="empty-state">Aucun bloc d'audit disponible pour ce document.</div>
              )}
            </article>

            <article className="panel side-panel">
              <p className="panel-label">Analyse</p>
              <h3>Lecture de l'audit</h3>
              <div className="stack-list">
                <div className="stack-row stack-row-static">
                  <span>Blocs coherents</span>
                  <strong>{formatNumber((result.auditBlocs || []).filter((block) => block.coherent).length)}</strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Blocs a relire</span>
                  <strong>{formatNumber((result.auditBlocs || []).filter((block) => !block.coherent).length)}</strong>
                </div>
                <div className="stack-row stack-row-static">
                  <span>Lignes affichees</span>
                  <strong>{formatNumber(filteredLines.length)}</strong>
                </div>
              </div>
            </article>
          </section>

          <section className="panel filters-panel">
            <div className="panel-heading">
              <div>
                <p className="panel-label">Filtres</p>
                <h3>Audit dynamique des lignes</h3>
              </div>
              <div className="builder-actions">
                <button
                  className="ghost-button"
                  type="button"
                  onClick={() => {
                    setLotFilter("");
                    setBatimentFilter("");
                    setNiveauFilter("");
                    setStatusFilter("");
                  }}
                >
                  Reinitialiser filtres
                </button>
                <button className="ghost-button" type="button" onClick={dqeFullState.resetCorrections}>
                  Annuler corrections
                </button>
              </div>
            </div>

            <div className="filters-grid filters-grid-four">
              <FilterSelect label="Lot" value={lotFilter} options={lotOptions} onChange={setLotFilter} />
              <FilterSelect label="Batiment" value={batimentFilter} options={batimentOptions} onChange={setBatimentFilter} />
              <FilterSelect label="Niveau" value={niveauFilter} options={niveauOptions} onChange={setNiveauFilter} />
              <FilterSelect
                label="Statut"
                value={statusFilter}
                options={[
                  { label: "Valides", value: "VALIDES" },
                  { label: "En erreurs", value: "ERREURS" },
                  { label: "Sans batiment", value: "SANS_BATIMENT" },
                  { label: "Sans niveau", value: "SANS_NIVEAU" },
                  { label: "Non classees", value: "NON_CLASSEES" },
                ]}
                onChange={setStatusFilter}
              />
            </div>
          </section>

          <section className="panel table-panel">
            <div className="panel-heading">
              <div>
                <p className="panel-label">Preview</p>
                <h3>Lignes structurees et enrichies</h3>
              </div>
              <span className="table-count">{filteredLines.length} ligne(s)</span>
            </div>

            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Designation</th>
                    <th>Batiment</th>
                    <th>Niveau</th>
                    <th>Lot</th>
                    <th>Famille</th>
                    <th>Quantite</th>
                    <th>Unite</th>
                    <th>Prix local</th>
                    <th>Prix import FOB</th>
                    <th>Import rendu</th>
                    <th>Decision</th>
                    <th>Risque</th>
                    <th>Confiance</th>
                    <th>Erreurs</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredLines.map((line) => (
                    <tr key={`${line.designation}-${line.sourceIndex}`}>
                      <td>
                        <input
                          className="table-edit-input table-edit-wide"
                          value={line.designation || ""}
                          onChange={(event) => dqeFullState.updateLine(line.sourceIndex, "designation", event.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          className="table-edit-input"
                          value={line.batiment || ""}
                          onChange={(event) => dqeFullState.updateLine(line.sourceIndex, "batiment", event.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          className="table-edit-input"
                          value={line.niveau || ""}
                          onChange={(event) => dqeFullState.updateLine(line.sourceIndex, "niveau", event.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          className="table-edit-input"
                          value={line.lot || ""}
                          onChange={(event) => dqeFullState.updateLine(line.sourceIndex, "lot", event.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          className="table-edit-input"
                          value={line.famille || ""}
                          onChange={(event) => dqeFullState.updateLine(line.sourceIndex, "famille", event.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          className="table-edit-input table-edit-number"
                          type="number"
                          step="0.01"
                          value={line.quantite ?? ""}
                          onChange={(event) => dqeFullState.updateLine(line.sourceIndex, "quantite", event.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          className="table-edit-input"
                          value={line.unite || ""}
                          onChange={(event) => dqeFullState.updateLine(line.sourceIndex, "unite", event.target.value)}
                        />
                      </td>
                      <td>
                        <input
                          className="table-edit-input table-edit-number"
                          type="number"
                          step="0.01"
                          value={line.prixLocalEstime ?? line.prixUnitaire ?? ""}
                          onChange={(event) => dqeFullState.updateLine(line.sourceIndex, "prixLocalEstime", event.target.value)}
                        />
                      </td>
                      <td>{line.prixImportEstime == null ? "-" : formatCurrency(line.prixImportEstime, currencyCode)}</td>
                      <td>{line.prixImportRendu == null ? "-" : formatCurrency(line.prixImportRendu, currencyCode)}</td>
                      <td>
                        <span className={`decision-badge ${getDecisionClass(line.decision)}`}>
                          {line.decision || "-"}
                        </span>
                      </td>
                      <td>
                        <span className={`decision-badge ${getRiskClass(line.risque)}`}>
                          {line.risque || "-"}
                        </span>
                      </td>
                      <td>
                        <span className={`decision-badge ${getScoreClass(line.scoreConfiance)}`}>
                          {formatNumber(line.scoreConfiance)}
                        </span>
                      </td>
                      <td>{line.erreurs?.join(", ") || "-"}</td>
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
