import {
  BatimentNiveauHeatmapPanel,
  ChartPanel,
  DqeBuilderPanel,
  DqeDocumentImportPanel,
  DqeDraftTable,
  ErrorPanel,
  FamilyPanel,
  FilterPanel,
  ImportPanel,
  LoadingPanel,
  PageHeader,
  RecentItemsPanel,
  StructureBreakdownPanel,
} from "../components/DashboardBits";

export default function Import({ dashboard }) {
  const { filters, data, importState, projectContext, dqeState } = dashboard;
  const currencyCode = projectContext.activeCurrencyCode;
  const coverage = data.coverageMetrics;

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Import"
        title="Sourcing et matrices DQE"
        description="Un espace operationnel pour charger les fichiers Excel, choisir le projet cible et analyser rapidement les familles les plus consommatrices."
        backendHint={data.backendStatusMessage}
        backendHintTone={data.backendWakeInProgress ? "warning" : "info"}
      />

      <section className="top-grid">
        <FilterPanel filters={filters} data={data} projectContext={projectContext} />
        <ImportPanel importState={importState} projectContext={projectContext} />
      </section>

      <section className="top-grid">
        <DqeBuilderPanel dqeState={dqeState} projectContext={projectContext} currencyCode={currencyCode} />
        <DqeDocumentImportPanel dqeState={dqeState} projectContext={projectContext} />
      </section>

      <DqeDraftTable dqeState={dqeState} currencyCode={currencyCode} />

      {(data.loading || data.catalogLoading) && <LoadingPanel message="Chargement de l'espace import..." />}
      <ErrorPanel error={data.error} backendHint={data.backendStatusMessage} />

      <section className="content-grid">
        <ChartPanel chartData={data.chartData} onBarClick={filters.setLotFilter} currencyCode={currencyCode} />
        <FamilyPanel familyEntries={data.familyEntries} onSelect={filters.setFamilleFilter} currencyCode={currencyCode} />
      </section>

      <section className="three-grid">
        <article className="panel insight-card">
          <p className="panel-label">Couverture</p>
          <h3>Lignes DQE actives</h3>
          <strong>{coverage.totalLines}</strong>
          <p className="helper-text">Nombre de lignes deja chargees dans le projet actif.</p>
        </article>

        <article className="panel insight-card">
          <p className="panel-label">Couverture</p>
          <h3>Lots reconnus</h3>
          <strong>{coverage.lots}</strong>
          <p className="helper-text">Diversite des lots utilises par les dashboards.</p>
        </article>

        <article className="panel insight-card">
          <p className="panel-label">Couverture</p>
          <h3>Familles reconnues</h3>
          <strong>{coverage.families}</strong>
          <p className="helper-text">Granularite des familles prêtes pour le sourcing.</p>
        </article>
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

      <RecentItemsPanel recentItems={data.recentItems} title="Dernieres lignes importees" currencyCode={currencyCode} />
    </div>
  );
}
