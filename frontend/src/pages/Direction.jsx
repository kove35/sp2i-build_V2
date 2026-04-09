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
      />

      <FilterPanel filters={filters} data={data} projectContext={projectContext} />

      {(data.loading || data.catalogLoading) && (
        <LoadingPanel message="Chargement du dashboard direction..." />
      )}
      {!data.loading && <ErrorPanel error={data.error} />}

      {!data.loading && !data.error && (
        <>
          <MetricsGrid summary={data.summary} currencyCode={currencyCode} />

          <section className="content-grid">
            <ChartPanel chartData={data.chartData} onBarClick={filters.setLotFilter} currencyCode={currencyCode} />
            <FamilyPanel familyEntries={data.familyEntries} onSelect={filters.setFamilleFilter} currencyCode={currencyCode} />
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

          <TopEconomiesPanel items={data.topEconomies} currencyCode={currencyCode} />
        </>
      )}
    </div>
  );
}
