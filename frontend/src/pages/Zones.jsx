import {
  BatimentNiveauHeatmapPanel,
  ErrorPanel,
  FilterPanel,
  ItemsTable,
  LoadingPanel,
  PageHeader,
} from "../components/DashboardBits";
import { formatCurrency } from "../lib/capex";

export default function Zones({ dashboard }) {
  const { filters, data, projectContext } = dashboard;
  const currencyCode = projectContext.activeCurrencyCode;
  const zoneEntries = data.zoneEntries;
  const coverage = data.coverageMetrics;

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Zones"
        title="Dashboard batiment / niveau"
        description="Une lecture spatiale du projet pour voir ou se concentre le CAPEX, comparer les zones et filtrer rapidement par batiment ou niveau."
      />

      <FilterPanel filters={filters} data={data} projectContext={projectContext} />

      {(data.loading || data.catalogLoading) && (
        <LoadingPanel message="Chargement du dashboard batiment / niveau..." />
      )}
      {!data.loading && <ErrorPanel error={data.error} />}

      {!data.loading && !data.error && (
        <>
          <section className="three-grid">
            <article className="panel insight-card">
              <p className="panel-label">Structure</p>
              <h3>Batiments couverts</h3>
              <strong>{coverage.buildings}</strong>
              <p className="helper-text">Nombre de batiments identifies dans le projet actif.</p>
            </article>

            <article className="panel insight-card">
              <p className="panel-label">Structure</p>
              <h3>Niveaux couverts</h3>
              <strong>{coverage.levels}</strong>
              <p className="helper-text">Nombre de niveaux ou zones exploitables dans les dashboards.</p>
            </article>

            <article className="panel insight-card">
              <p className="panel-label">Structure</p>
              <h3>Zones actives</h3>
              <strong>{zoneEntries.length}</strong>
              <p className="helper-text">Combinaisons batiment / niveau visibles avec les filtres courants.</p>
            </article>
          </section>

          <BatimentNiveauHeatmapPanel
            heatmap={data.batimentNiveauHeatmap}
            currencyCode={currencyCode}
            onSelect={(batiment, niveau) => {
              filters.setBatimentFilter(batiment);
              filters.setNiveauFilter(niveau);
            }}
          />

          <section className="content-grid">
            <article className="panel">
              <div className="panel-heading">
                <div>
                  <p className="panel-label">Top zones</p>
                  <h3>CAPEX par zone</h3>
                </div>
              </div>

              <div className="stack-list">
                {zoneEntries.length === 0 && <div className="empty-state">Aucune zone disponible.</div>}

                {zoneEntries.map((zone) => (
                  <button
                    key={zone.key}
                    type="button"
                    className="stack-row"
                    onClick={() => {
                      filters.setBatimentFilter(zone.batiment);
                      filters.setNiveauFilter(zone.niveau);
                    }}
                  >
                    <span>{zone.batimentLabel} | {zone.niveauLabel}</span>
                    <strong>{formatCurrency(zone.capexBrut, currencyCode)}</strong>
                  </button>
                ))}
              </div>
            </article>

            <article className="panel">
              <div className="panel-heading">
                <div>
                  <p className="panel-label">Lecture</p>
                  <h3>Lots et familles par zone</h3>
                </div>
              </div>

              <div className="stack-list">
                {zoneEntries.length === 0 && <div className="empty-state">Aucune zone disponible.</div>}

                {zoneEntries.slice(0, 8).map((zone) => (
                  <div key={`${zone.key}-detail`} className="stack-row stack-row-static zone-detail-row">
                    <span>
                      {zone.batimentLabel} | {zone.niveauLabel}
                    </span>
                    <strong>{zone.count} ligne(s)</strong>
                    <small className="zone-detail-copy">
                      Lots : {zone.lots.slice(0, 3).join(", ") || "-"}
                      <br />
                      Familles : {zone.families.slice(0, 3).join(", ") || "-"}
                    </small>
                  </div>
                ))}
              </div>
            </article>
          </section>

          <ItemsTable items={data.items} title="Postes CAPEX par zone" currencyCode={currencyCode} />
        </>
      )}
    </div>
  );
}
