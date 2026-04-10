import {
  BatimentNiveauHeatmapPanel,
  ChartPanel,
  ErrorPanel,
  FamilyPanel,
  FilterPanel,
  LoadingPanel,
  MetricsGrid,
  PageHeader,
  StructureBreakdownPanel,
  TopEconomiesPanel,
} from "../components/DashboardBits";

export default function Direction({ dashboard }) {
  const { filters, data, projectContext } = dashboard;
  const currencyCode = projectContext.activeCurrencyCode;
  const coverage = data.coverageMetrics;

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Direction"
        title="Synthese executive CAPEX"
        description="Une vue de direction pour comprendre les KPI, les leviers d'economie et les grandes masses par lot et par famille."
        backendHint={data.backendStatusMessage}
        backendHintTone={data.backendWakeInProgress ? "warning" : "info"}
      />

      <FilterPanel filters={filters} data={data} projectContext={projectContext} />

      {(data.loading || data.catalogLoading) && (
        <LoadingPanel message="Chargement du dashboard direction..." />
      )}
      {!data.loading && <ErrorPanel error={data.error} backendHint={data.backendStatusMessage} />}

      {!data.loading && !data.error && (
        <>
          <section className="section-copy-card">
            <p className="panel-label">Decision & pilotage</p>
            <h3>Synthese executive</h3>
            <p className="helper-text">
              Cette section rassemble les KPI de direction, les masses CAPEX et les principaux
              leviers d'arbitrage pour le projet actif.
            </p>
          </section>

          <MetricsGrid summary={data.summary} currencyCode={currencyCode} />

          <section className="content-grid">
            <ChartPanel chartData={data.chartData} onBarClick={filters.setLotFilter} currencyCode={currencyCode} />
            <FamilyPanel familyEntries={data.familyEntries} onSelect={filters.setFamilleFilter} currencyCode={currencyCode} />
          </section>

          <section className="section-copy-card">
            <p className="panel-label">Decision & pilotage</p>
            <h3>Couverture du projet</h3>
            <p className="helper-text">
              Une lecture rapide de la profondeur du modele : lignes exploitees, batiments couverts
              et niveaux effectivement presentes dans le projet.
            </p>
          </section>

          <section className="three-grid">
            <article className="panel insight-card">
              <p className="panel-label">Couverture</p>
              <h3>Lignes exploitees</h3>
              <strong>{coverage.totalLines}</strong>
              <p className="helper-text">Nombre de lignes actuellement visibles dans le dashboard.</p>
            </article>

            <article className="panel insight-card">
              <p className="panel-label">Couverture</p>
              <h3>Batiments couverts</h3>
              <strong>{coverage.buildings}</strong>
              <p className="helper-text">Nombre de batiments renseignes par les donnees importees.</p>
            </article>

            <article className="panel insight-card">
              <p className="panel-label">Couverture</p>
              <h3>Niveaux couverts</h3>
              <strong>{coverage.levels}</strong>
              <p className="helper-text">Nombre de niveaux ou zones couverts dans le projet actif.</p>
            </article>
          </section>

          <section className="section-copy-card">
            <p className="panel-label">Decision & pilotage</p>
            <h3>Lecture spatiale CAPEX</h3>
            <p className="helper-text">
              On descend ici par batiment, par niveau puis par croisement zone x niveau pour
              identifier les poches de cout les plus importantes.
            </p>
          </section>

          <section className="content-grid">
            <StructureBreakdownPanel
              title="CAPEX par batiment"
              entries={data.batimentEntries}
              onSelect={filters.setBatimentFilter}
              currencyCode={currencyCode}
            />

            <StructureBreakdownPanel
              title="CAPEX par niveau"
              entries={data.niveauEntries}
              onSelect={filters.setNiveauFilter}
              currencyCode={currencyCode}
            />
          </section>

          <BatimentNiveauHeatmapPanel
            heatmap={data.batimentNiveauHeatmap}
            currencyCode={currencyCode}
            onSelect={(batiment, niveau) => {
              filters.setBatimentFilter(batiment);
              filters.setNiveauFilter(niveau);
            }}
          />

          <section className="section-copy-card">
            <p className="panel-label">Decision & pilotage</p>
            <h3>Arbitrages prioritaires</h3>
            <p className="helper-text">
              Les lignes ci-dessous mettent en avant les plus gros gains potentiels pour guider la
              decision executive.
            </p>
          </section>

          <TopEconomiesPanel items={data.topEconomies} currencyCode={currencyCode} />
        </>
      )}
    </div>
  );
}
