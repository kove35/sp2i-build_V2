import {
  BatimentNiveauHeatmapPanel,
  ChartPanel,
  ErrorPanel,
  FamilyPanel,
  FilterPanel,
  ItemsTable,
  LoadingPanel,
  MetricsGrid,
  PageHeader,
  StructureBreakdownPanel,
} from "../components/DashboardBits";
import { formatCurrency, formatNumber, getDecision, getDecisionVariant } from "../lib/capex";

export default function Chantier({ dashboard }) {
  const { filters, data, projectContext } = dashboard;
  const currencyCode = projectContext.activeCurrencyCode;
  const coverage = data.coverageMetrics;

  const importCount = data.items.filter((item) => getDecision(item) === "IMPORT").length;
  const localCount = data.items.filter((item) => getDecisionVariant(item) === "local").length;
  const mixCount = data.items.filter((item) => getDecisionVariant(item) === "mix").length;
  const totalQuantity = data.items.reduce((sum, item) => sum + (item.quantite ?? 0), 0);

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Chantier"
        title="Couts terrain et arbitrages"
        description="Une vision terrain pour comparer les couts locaux et import, suivre les volumes engages et voir rapidement quelles decisions dominent."
      />

      <FilterPanel filters={filters} data={data} projectContext={projectContext} />

      {(data.loading || data.catalogLoading) && (
        <LoadingPanel message="Chargement du dashboard chantier..." />
      )}
      {!data.loading && <ErrorPanel error={data.error} />}

      {!data.loading && !data.error && (
        <>
          <MetricsGrid summary={data.summary} mode="chantier" currencyCode={currencyCode} />

          <section className="three-grid">
            <article className="panel insight-card">
              <p className="panel-label">Operations</p>
              <h3>Decisions import</h3>
              <strong>{importCount}</strong>
              <p className="helper-text">Nombre de lignes ou l'import est plus favorable.</p>
            </article>

            <article className="panel insight-card">
              <p className="panel-label">Operations</p>
              <h3>Decisions local</h3>
              <strong>{localCount}</strong>
              <p className="helper-text">Nombre de lignes ou le local reste le meilleur choix.</p>
            </article>

            <article className="panel insight-card">
              <p className="panel-label">Volume</p>
              <h3>Zone mix</h3>
              <strong>{mixCount}</strong>
              <p className="helper-text">Lignes proches entre local et import, a arbitrer finement.</p>
            </article>

            <article className="panel insight-card">
              <p className="panel-label">Volume</p>
              <h3>Quantite totale</h3>
              <strong>{formatNumber(totalQuantity)}</strong>
              <p className="helper-text">Cumule des quantites sur le perimetre filtre.</p>
            </article>

            <article className="panel insight-card">
              <p className="panel-label">Structure</p>
              <h3>Batiments suivis</h3>
              <strong>{coverage.buildings}</strong>
              <p className="helper-text">Nombre de batiments couverts par les lignes du chantier.</p>
            </article>

            <article className="panel insight-card">
              <p className="panel-label">Structure</p>
              <h3>Niveaux suivis</h3>
              <strong>{coverage.levels}</strong>
              <p className="helper-text">Nombre de niveaux ou zones presents dans la selection.</p>
            </article>
          </section>

          <section className="content-grid">
            <ChartPanel chartData={data.chartData} onBarClick={filters.setLotFilter} currencyCode={currencyCode} />
            <FamilyPanel familyEntries={data.familyEntries} onSelect={filters.setFamilleFilter} currencyCode={currencyCode} />
          </section>

          <section className="content-grid">
            <StructureBreakdownPanel
              title="Lecture terrain par batiment"
              entries={data.batimentEntries}
              onSelect={filters.setBatimentFilter}
              currencyCode={currencyCode}
            />

            <StructureBreakdownPanel
              title="Lecture terrain par niveau"
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

          <ItemsTable items={data.items} title="Postes CAPEX terrain" currencyCode={currencyCode} />
        </>
      )}
    </div>
  );
}
