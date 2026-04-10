import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  BACKEND_LABEL,
  calculateGain,
  formatCurrency,
  formatNumber,
  formatPercent,
  getDecisionLabel,
  getDecisionVariant,
  normalizeFamilyLabel,
  normalizeLotLabel,
  normalizeZoneLabel,
} from "../lib/capex";

const TOOLTIP_CONTENT_STYLE = {
  backgroundColor: "#111827",
  border: "1px solid #334155",
  borderRadius: "12px",
  color: "#f8fafc",
};

const TOOLTIP_TEXT_STYLE = {
  color: "#f8fafc",
};

export function PageHeader({ eyebrow, title, description, backendHint = "", backendHintTone = "info" }) {
  return (
    <section className="page-hero">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h2>{title}</h2>
        <p className="hero-copy">{description}</p>
        {backendHint && <p className={`hero-hint hero-hint-${backendHintTone}`}>{backendHint}</p>}
      </div>
      <div className="hero-status">
        <span className="status-dot" />
        Backend connecte sur {BACKEND_LABEL}
      </div>
    </section>
  );
}

export function LoadingPanel({ message }) {
  return (
    <section className="panel loading-panel">
      <div className="spinner" />
      <p>{message}</p>
    </section>
  );
}

export function FilterSelect({ label, value, options, onChange, placeholder = "Tous" }) {
  const normalizedOptions = options.map((option) =>
    typeof option === "string" ? { label: option, value: option } : option
  );

  return (
    <label className="filter-field">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">{placeholder}</option>
        {normalizedOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

export function ProjectSelectorPanel({ projectContext }) {
  return (
    <section className="panel">
      <div className="panel-heading">
        <div>
          <p className="panel-label">Contexte</p>
          <h3>Projet actif</h3>
        </div>
      </div>

      <FilterSelect
        label="Projet de travail"
        value={projectContext.activeProjectId}
        options={projectContext.projects.map((project) => ({
          label: project.name,
          value: String(project.id),
        }))}
        onChange={projectContext.setActiveProjectId}
        placeholder="Choisir un projet"
      />
    </section>
  );
}

export function ActiveFilterChip({ label, value, onClear }) {
  if (!value) {
    return null;
  }

  return (
    <button className="filter-chip" type="button" onClick={onClear}>
      {label} : {value} x
    </button>
  );
}

export function FilterPanel({ filters, data, projectContext }) {
  return (
    <section className="panel filters-panel">
      <div className="panel-heading">
        <div>
          <p className="panel-label">Pilotage</p>
          <h3>Filtres dynamiques</h3>
        </div>
        <button className="ghost-button" type="button" onClick={filters.clearFilters}>
          Reinitialiser
        </button>
      </div>

      <div className="filters-grid filters-grid-five">
        <FilterSelect
          label="Projet"
          value={projectContext.activeProjectId}
          options={projectContext.projects.map((project) => ({
            label: project.name,
            value: String(project.id),
          }))}
          onChange={projectContext.setActiveProjectId}
          placeholder="Choisir un projet"
        />
        <FilterSelect
          label="Lot"
          value={filters.lotFilter}
          options={data.lotOptions}
          onChange={filters.setLotFilter}
        />
        <FilterSelect
          label="Famille"
          value={filters.familleFilter}
          options={data.familleOptions}
          onChange={filters.setFamilleFilter}
        />
        <FilterSelect
          label="Batiment"
          value={filters.batimentFilter}
          options={data.batimentOptions}
          onChange={filters.setBatimentFilter}
        />
        <FilterSelect
          label="Niveau"
          value={filters.niveauFilter}
          options={data.niveauOptions}
          onChange={filters.setNiveauFilter}
        />
      </div>

      <div className="active-filters">
        <ActiveFilterChip label="Lot" value={filters.lotFilter} onClear={() => filters.setLotFilter("")} />
        <ActiveFilterChip
          label="Famille"
          value={filters.familleFilter}
          onClear={() => filters.setFamilleFilter("")}
        />
        <ActiveFilterChip
          label="Batiment"
          value={filters.batimentFilter}
          onClear={() => filters.setBatimentFilter("")}
        />
        <ActiveFilterChip
          label="Niveau"
          value={filters.niveauFilter}
          onClear={() => filters.setNiveauFilter("")}
        />
      </div>
    </section>
  );
}

export function MetricCard({ title, value, accent, subtitle, detail }) {
  return (
    <article className={`metric-card accent-${accent}`}>
      <div>
        <p>{title}</p>
        <small>{subtitle}</small>
      </div>
      <div className="metric-main">
        <strong>{value}</strong>
        {detail && <span className="metric-detail">{detail}</span>}
      </div>
    </article>
  );
}

export function MetricsGrid({ summary, mode = "default", currencyCode = "XAF" }) {
  if (!summary) {
    return null;
  }

  const metrics =
    mode === "chantier"
      ? [
          {
            title: "Cout local terrain",
            value: formatCurrency(summary.capexBrut, currencyCode),
            accent: "cyan",
            subtitle: "Reference chantier",
            detail: `+${formatCurrency(summary.gainTotal, currencyCode)} | ${formatPercent(summary.taux)}`,
          },
          {
            title: "Scenario import",
            value: formatCurrency(summary.capexOptimise, currencyCode),
            accent: "green",
            subtitle: "Alternative comparee",
            detail: `${summary.nbArticlesSansPrixChine ?? 0} sans prix Chine`,
          },
          {
            title: "Gain terrain",
            value: formatCurrency(summary.economie, currencyCode),
            accent: "orange",
            subtitle: "Impact direct sur les couts",
            detail: `${formatCurrency(summary.capexSansPrixChine, currencyCode)} non optimisable`,
          },
          {
            title: "Levier activable",
            value: formatPercent(summary.taux),
            accent: "purple",
            subtitle: "Part optimisable",
            detail: `+${formatCurrency(summary.gainTotal, currencyCode)} potentiel`,
          },
        ]
      : [
          {
            title: "CAPEX brut",
            value: formatCurrency(summary.capexBrut, currencyCode),
            accent: "cyan",
            subtitle: "Somme des couts locaux",
            detail: `+${formatCurrency(summary.gainTotal, currencyCode)} | ${formatPercent(summary.taux)}`,
          },
          {
            title: "CAPEX optimise",
            value: formatCurrency(summary.capexOptimise, currencyCode),
            accent: "green",
            subtitle: "Scenario le plus economique",
            detail: `${formatCurrency(summary.economie, currencyCode)} d'economie`,
          },
          {
            title: "Articles sans prix Chine",
            value: formatNumber(summary.nbArticlesSansPrixChine),
            accent: "orange",
            subtitle: "Lignes a completer",
            detail: `${formatCurrency(summary.capexSansPrixChine, currencyCode)} non optimisable`,
          },
          {
            title: "Taux d'optimisation",
            value: formatPercent(summary.taux),
            accent: "purple",
            subtitle: "Part d'economie vs brut",
            detail: `Gain total ${formatCurrency(summary.gainTotal, currencyCode)}`,
          },
        ];

  return (
    <section className="metrics-grid">
      {metrics.map((metric) => (
        <MetricCard key={metric.title} {...metric} />
      ))}
    </section>
  );
}

export function ChartPanel({ chartData, onBarClick, currencyCode = "XAF" }) {
  const minChartWidth = Math.max(620, chartData.length * 108);

  return (
    <article className="panel chart-panel">
      <div className="panel-heading">
        <div>
          <p className="panel-label">Analyse</p>
          <h3>CAPEX par lot</h3>
        </div>
        <p className="helper-text">Clique sur une barre pour filtrer globalement le dashboard.</p>
      </div>

      {chartData.length === 0 ? (
        <div className="empty-state">Aucun lot disponible pour la selection courante.</div>
      ) : (
        <div className="chart-scroll chart-scroll-x">
          <div className="chart-canvas" style={{ minWidth: `${minChartWidth}px` }}>
            <ResponsiveContainer width="100%" height={288}>
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#27324a" />
                <XAxis dataKey="label" stroke="#94a3b8" tick={{ fill: "#cbd5e1", fontSize: 11 }} />
                <YAxis stroke="#94a3b8" tick={{ fill: "#cbd5e1", fontSize: 11 }} />
                <Tooltip
                  contentStyle={TOOLTIP_CONTENT_STYLE}
                  itemStyle={TOOLTIP_TEXT_STYLE}
                  labelStyle={TOOLTIP_TEXT_STYLE}
                  formatter={(value, key, payload) => {
                    if (key === "capexBrut") {
                      return [formatCurrency(value, currencyCode), "Montant"];
                    }
                      return [formatCurrency(value, currencyCode), payload?.payload?.label];
                    }}
                    labelFormatter={(label, payload) => {
                      const row = payload?.[0]?.payload;
                      if (!row) {
                        return label;
                      }
                      return `${row.label} | +${formatCurrency(row.gainTotal, currencyCode)} | ${formatPercent(row.taux)}`;
                    }}
                  />
                <Bar dataKey="capexBrut" radius={[10, 10, 0, 0]} onClick={(data) => onBarClick(data.name)}>
                  {chartData.map((entry) => (
                    <Cell
                      key={entry.name}
                      fill={entry.isActive ? "#22c55e" : "#22d3ee"}
                      cursor="pointer"
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </article>
  );
}

export function FamilyPanel({ familyEntries, onSelect, currencyCode = "XAF" }) {
  const chartData = familyEntries.slice(0, 8).map((entry) => ({
    ...entry,
    label: entry.label || entry.name,
  }));
  const chartHeight = Math.max(260, chartData.length * 52);

  return (
    <article className="panel side-panel">
      <p className="panel-label">Vue rapide</p>
      <h3>Repartition familles</h3>
      <div className="chart-wrapper chart-wrapper-compact chart-scroll chart-scroll-y">
        {chartData.length === 0 ? (
          <div className="empty-state">Aucune famille disponible.</div>
        ) : (
          <div className="chart-canvas" style={{ height: `${chartHeight}px` }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={chartData}
                layout="vertical"
                margin={{ top: 8, right: 10, left: 10, bottom: 8 }}
              >
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(97, 118, 148, 0.18)" />
                <XAxis
                  type="number"
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#9fb0c8", fontSize: 11 }}
                />
                <YAxis
                  type="category"
                  dataKey="label"
                  width={158}
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#d7e3f8", fontSize: 11 }}
                />
                <Tooltip
                  cursor={{ fill: "rgba(23, 201, 100, 0.08)" }}
                  contentStyle={TOOLTIP_CONTENT_STYLE}
                  itemStyle={TOOLTIP_TEXT_STYLE}
                  labelStyle={TOOLTIP_TEXT_STYLE}
                  formatter={(value) => formatCurrency(value, currencyCode)}
                  labelFormatter={(label) => label}
                />
                <Bar dataKey="capexBrut" radius={[0, 8, 8, 0]} onClick={(payload) => onSelect(payload.name)}>
                  {chartData.map((entry) => (
                    <Cell key={entry.name} fill={entry.isActive ? "#23c16b" : "#35c5dd"} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </article>
  );
}

export function StructureBreakdownPanel({
  title,
  entries,
  onSelect,
  currencyCode = "XAF",
}) {
  const chartData = entries.slice(0, 8).map((entry) => ({
    ...entry,
    label: entry.label || entry.name,
  }));
  const chartHeight = Math.max(260, chartData.length * 52);

  return (
    <article className="panel">
      <div className="panel-heading">
        <div>
          <p className="panel-label">Structure</p>
          <h3>{title}</h3>
        </div>
      </div>

      <div className="chart-wrapper chart-wrapper-compact chart-scroll chart-scroll-y">
        {chartData.length === 0 ? (
          <div className="empty-state">Aucune donnee disponible.</div>
        ) : (
          <div className="chart-canvas" style={{ height: `${chartHeight}px` }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={chartData}
                layout="vertical"
                margin={{ top: 8, right: 10, left: 10, bottom: 8 }}
              >
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(97, 118, 148, 0.18)" />
                <XAxis
                  type="number"
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#9fb0c8", fontSize: 11 }}
                />
                <YAxis
                  type="category"
                  dataKey="label"
                  width={148}
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#d7e3f8", fontSize: 11 }}
                />
                <Tooltip
                  cursor={{ fill: "rgba(53, 197, 221, 0.08)" }}
                  contentStyle={TOOLTIP_CONTENT_STYLE}
                  itemStyle={TOOLTIP_TEXT_STYLE}
                  labelStyle={TOOLTIP_TEXT_STYLE}
                  formatter={(value, key, payload) => {
                    if (key === "capexBrut") {
                      return [formatCurrency(value, currencyCode), "Montant"];
                    }
                    return [value, payload?.payload?.label];
                  }}
                  labelFormatter={(label, payload) => {
                    const row = payload?.[0]?.payload;
                    if (!row) {
                      return label;
                    }
                    return `${row.label} | ${row.count} ligne(s)`;
                  }}
                />
                <Bar dataKey="capexBrut" radius={[0, 8, 8, 0]} onClick={(payload) => onSelect(payload.name)}>
                  {chartData.map((entry) => (
                    <Cell key={entry.name} fill={entry.isActive ? "#23c16b" : "#35c5dd"} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </article>
  );
}

export function TopEconomiesPanel({ items, currencyCode = "XAF" }) {
  return (
    <article className="panel">
      <div className="panel-heading">
        <div>
          <p className="panel-label">Priorites</p>
          <h3>Top 5 economies</h3>
        </div>
      </div>

      <div className="stack-list">
        {items.length === 0 && <div className="empty-state">Aucune economie exploitable pour le moment.</div>}

          {items.map((item) => (
            <div className="stack-row stack-row-static" key={item.id}>
              <span>
                {normalizeLotLabel(item.lot)} | {normalizeFamilyLabel(item.famille)}
              </span>
              <strong>{formatCurrency(item.gainTotal, currencyCode)}</strong>
            </div>
          ))}
      </div>
    </article>
  );
}

export function BatimentNiveauHeatmapPanel({
  heatmap,
  onSelect,
  currencyCode = "XAF",
}) {
  const { batiments = [], niveaux = [], maxValue = 0, cells = [] } = heatmap || {};
  const gridTemplateColumns = `220px repeat(${niveaux.length}, minmax(170px, 1fr))`;

  const findCell = (batiment, niveau) =>
    cells.find((cell) => cell.batiment === batiment && cell.niveau === niveau);

  return (
    <article className="panel">
      <div className="panel-heading">
        <div>
          <p className="panel-label">Heatmap</p>
          <h3>CAPEX batiment x niveau</h3>
        </div>
        <p className="helper-text">Clique sur une case pour filtrer la zone correspondante.</p>
      </div>

      {batiments.length === 0 || niveaux.length === 0 ? (
        <div className="empty-state">Aucune matrice batiment / niveau disponible.</div>
      ) : (
        <div className="heatmap-shell">
          <div className="heatmap-legend">
            <span className="heatmap-legend-label">Intensite CAPEX</span>
            <div className="heatmap-legend-scale" aria-hidden="true" />
            <div className="heatmap-legend-values">
              <span>Faible</span>
              <span>Forte</span>
            </div>
          </div>

          <div className="heatmap-scroll">
          <div className="heatmap-grid">
            <div className="heatmap-row heatmap-header" style={{ gridTemplateColumns }}>
              <div className="heatmap-corner">Batiment / Niveau</div>
              {niveaux.map((niveau) => (
                <div className="heatmap-column-label" key={niveau.name}>
                  {niveau.label}
                </div>
              ))}
            </div>

            {batiments.map((batiment) => (
              <div className="heatmap-row" key={batiment.name} style={{ gridTemplateColumns }}>
                <div className="heatmap-row-label">{batiment.label}</div>
                {niveaux.map((niveau) => {
                  const cell = findCell(batiment.name, niveau.name);
                  const intensity = maxValue > 0 && cell ? Math.max(cell.capexBrut / maxValue, 0.08) : 0.04;
                  const isEmpty = !cell;

                  return (
                    <button
                      key={`${batiment.name}-${niveau.name}`}
                      type="button"
                      className={`heatmap-cell ${cell?.isActive ? "heatmap-cell-active" : ""} ${isEmpty ? "heatmap-cell-empty" : ""}`}
                    style={{
                      background: isEmpty
                        ? "rgba(15, 23, 42, 0.52)"
                        : `linear-gradient(135deg, rgba(34, 211, 238, ${intensity}), rgba(34, 197, 94, ${Math.min(
                        intensity + 0.12,
                        0.95
                      )}))`,
                    }}
                      title={
                        cell
                          ? `${batiment.label} | ${niveau.label} | ${formatCurrency(cell.capexBrut, currencyCode)} | ${cell.count} ligne(s)`
                          : `${batiment.label} | ${niveau.label} | aucun poste`
                      }
                      onClick={() => onSelect(batiment.name, niveau.name)}
                    >
                    {cell ? (
                      <>
                        <strong>{formatCurrency(cell.capexBrut, currencyCode)}</strong>
                        <span>{cell.count} ligne(s)</span>
                      </>
                    ) : (
                      <>
                        <strong className="heatmap-empty-value">-</strong>
                        <span>Aucune ligne</span>
                      </>
                    )}
                  </button>
                );
              })}
            </div>
          ))}
        </div>
        </div>
        </div>
      )}
    </article>
  );
}

export function ItemsTable({ items, title = "Liste des postes CAPEX", currencyCode = "XAF" }) {
  return (
    <section className="panel table-panel">
      <div className="panel-heading">
        <div>
          <p className="panel-label">Base de donnees</p>
          <h3>{title}</h3>
        </div>
        <span className="table-count">{items.length} item(s)</span>
      </div>

      {items.length === 0 ? (
        <div className="empty-state">Aucun item ne correspond aux filtres selectionnes.</div>
      ) : (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Projet</th>
                <th>Lot</th>
                <th>Famille</th>
                <th>Designation</th>
                <th>Unite</th>
                <th>Batiment</th>
                <th>Niveau</th>
                <th>Quantite</th>
                <th>Prix unitaire</th>
                <th>Prix total</th>
                <th>Cout local</th>
                <th>Cout import</th>
                <th>Gain</th>
                <th>Decision</th>
                <th>Statut Chine</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <td>{item.id}</td>
                  <td>{item.projectId ?? "-"}</td>
                  <td>{normalizeLotLabel(item.lot)}</td>
                  <td>{normalizeFamilyLabel(item.famille)}</td>
                  <td>{item.designation || "-"}</td>
                  <td>{item.unite || "-"}</td>
                  <td>{normalizeZoneLabel(item.batiment)}</td>
                  <td>{normalizeZoneLabel(item.niveau)}</td>
                  <td>{formatNumber(item.quantite)}</td>
                  <td>{formatCurrency(item.prixUnitaire ?? item.coutLocal, currencyCode)}</td>
                  <td>{formatCurrency(item.prixTotal ?? (item.quantite ?? 0) * (item.prixUnitaire ?? item.coutLocal ?? 0), currencyCode)}</td>
                  <td>{formatCurrency(item.coutLocal, currencyCode)}</td>
                  <td>{item.coutImport == null ? "-" : formatCurrency(item.coutImport, currencyCode)}</td>
                  <td>{formatCurrency(calculateGain(item), currencyCode)}</td>
                  <td>
                    <span className={`decision-badge decision-${getDecisionVariant(item)}`}>
                      {getDecisionLabel(item)}
                    </span>
                  </td>
                  <td>{item.statutPrixChine || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export function RecentItemsPanel({ recentItems, title = "Dernieres lignes ajoutees", currencyCode = "XAF" }) {
  return (
    <section className="panel recent-panel">
      <div className="panel-heading">
        <div>
          <p className="panel-label">Controle</p>
          <h3>{title}</h3>
        </div>
        <span className="table-count">{recentItems.length} ligne(s)</span>
      </div>

      {recentItems.length === 0 ? (
        <div className="empty-state">Aucune ligne recente a afficher.</div>
      ) : (
        <div className="recent-grid">
          {recentItems.map((item) => (
            <article className="recent-card" key={item.id}>
              <div className="recent-header">
                <strong>#{item.id}</strong>
                <span>Projet {item.projectId ?? "-"}</span>
              </div>
                <p>{normalizeLotLabel(item.lot)}</p>
                <small>{item.designation || normalizeFamilyLabel(item.famille)}</small>
                <div className="recent-meta">
                  <span>{normalizeFamilyLabel(item.famille)}</span>
                  <span>{item.unite || "-"}</span>
                </div>
              <div className="recent-footer">
                <span className={`decision-badge decision-${getDecisionVariant(item)}`}>
                  {getDecisionLabel(item)}
                </span>
                <div className="recent-value">{formatCurrency(item.prixTotal ?? item.coutLocal, currencyCode)}</div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export function ImportPanel({ importState, projectContext }) {
  return (
    <section className="panel import-panel">
      <div className="panel-heading">
        <div>
          <p className="panel-label">Import</p>
          <h3>Importer un DQE Excel</h3>
        </div>
        <button className="ghost-button" type="button" onClick={importState.handleTemplateDownload}>
          Telecharger le modele
        </button>
      </div>

      <div className="import-form">
        <FilterSelect
          label="Projet cible"
          value={importState.selectedProjectId || projectContext.activeProjectId}
          options={projectContext.projects.map((project) => ({
            label: project.name,
            value: String(project.id),
          }))}
          onChange={importState.setSelectedProjectId}
          placeholder="Choisir un projet"
        />

        <label className="filter-field">
          <span>Fichier Excel</span>
          <input
            className="file-input"
            type="file"
            accept=".xlsx,.xls"
            onChange={(event) => importState.setSelectedFile(event.target.files?.[0] ?? null)}
          />
        </label>

        <button
          className="primary-button"
          type="button"
          onClick={importState.handleImport}
          disabled={importState.importLoading}
        >
          {importState.importLoading ? "Import en cours..." : "Importer le DQE"}
        </button>
      </div>

      {importState.selectedFile && (
        <p className="helper-text">Fichier selectionne : {importState.selectedFile.name}</p>
      )}

      {importState.importMessage && <p className="success-text">{importState.importMessage}</p>}
      {importState.importError && <p className="error-text">{importState.importError}</p>}
    </section>
  );
}

export function DqeBuilderPanel({ dqeState, projectContext, currencyCode = "XAF" }) {
  return (
    <section className="panel dqe-builder-panel">
      <div className="panel-heading">
        <div>
          <p className="panel-label">Builder</p>
          <h3>Construire un DQE ligne par ligne</h3>
        </div>
        <button className="ghost-button" type="button" onClick={dqeState.handleDqeExport}>
          Export Excel
        </button>
      </div>

      <div className="filters-grid three-field-grid">
        <FilterSelect
          label="Projet cible"
          value={dqeState.selectedProjectId || projectContext.activeProjectId}
          options={projectContext.projects.map((project) => ({
            label: project.name,
            value: String(project.id),
          }))}
          onChange={dqeState.setSelectedProjectId}
          placeholder="Choisir un projet"
        />

        <label className="filter-field">
          <span>Lot</span>
          <input
            value={dqeState.draft.lot}
            onChange={(event) => dqeState.updateDraft("lot", event.target.value)}
            placeholder="Ex: Menuiserie"
          />
        </label>

        <label className="filter-field">
          <span>Famille</span>
          <input
            list="dqe-family-suggestions"
            value={dqeState.draft.famille}
            onChange={(event) => dqeState.updateDraft("famille", event.target.value)}
            placeholder="Ex: Fenetres"
          />
          <datalist id="dqe-family-suggestions">
            {dqeState.familySuggestions.map((family) => (
              <option key={family} value={family} />
            ))}
          </datalist>
        </label>
      </div>

      <div className="filters-grid dqe-builder-grid">
        <label className="filter-field">
          <span>Designation</span>
          <input
            value={dqeState.draft.designation}
            onChange={(event) => dqeState.updateDraft("designation", event.target.value)}
            placeholder="Libelle de la ligne DQE"
          />
        </label>

        <label className="filter-field">
          <span>Unite</span>
          <input
            value={dqeState.draft.unite}
            onChange={(event) => dqeState.updateDraft("unite", event.target.value)}
            placeholder="U / m2 / ml"
          />
        </label>

        <label className="filter-field">
          <span>Quantite</span>
          <input
            type="number"
            min="0"
            step="0.01"
            value={dqeState.draft.quantite}
            onChange={(event) => dqeState.updateDraft("quantite", event.target.value)}
            placeholder="0"
          />
        </label>

        <label className="filter-field">
          <span>Prix unitaire</span>
          <input
            type="number"
            min="0"
            step="0.01"
            value={dqeState.draft.prixUnitaire}
            onChange={(event) => dqeState.updateDraft("prixUnitaire", event.target.value)}
            placeholder="0"
          />
        </label>
      </div>

      {dqeState.familySuggestions.length > 0 && (
        <div className="active-filters">
          {dqeState.familySuggestions.map((family) => (
            <button
              key={family}
              type="button"
              className="filter-chip"
              onClick={() => dqeState.updateDraft("famille", family)}
            >
              {family}
            </button>
          ))}
        </div>
      )}

      <div className="builder-footer">
        <div className="builder-total-card">
          <span>Prix total calcule</span>
          <strong>{formatCurrency(dqeState.prixTotal, currencyCode)}</strong>
        </div>

        <div className="builder-actions">
          <button className="ghost-button" type="button" onClick={dqeState.addDraftLine}>
            Ajouter ligne
          </button>
          <button
            className="primary-button"
            type="button"
            onClick={dqeState.saveDraftLines}
            disabled={dqeState.builderLoading}
          >
            {dqeState.builderLoading ? "Enregistrement..." : "Sauvegarder le recap"}
          </button>
        </div>
      </div>

      {dqeState.builderMessage && <p className="success-text">{dqeState.builderMessage}</p>}
      {dqeState.builderError && <p className="error-text">{dqeState.builderError}</p>}
    </section>
  );
}

export function DqeDraftTable({ dqeState, currencyCode = "XAF" }) {
  return (
    <section className="panel table-panel">
      <div className="panel-heading">
        <div>
          <p className="panel-label">Recap</p>
          <h3>Lignes DQE en attente</h3>
        </div>
        <span className="table-count">{dqeState.draftLines.length} ligne(s)</span>
      </div>

      {dqeState.draftLines.length === 0 ? (
        <div className="empty-state">Ajoute des lignes DQE pour constituer ton recapitulatif.</div>
      ) : (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Lot</th>
                <th>Famille</th>
                <th>Designation</th>
                <th>Unite</th>
                <th>Quantite</th>
                <th>Prix unitaire</th>
                <th>Prix total</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {dqeState.draftLines.map((line, index) => (
                <tr key={`${line.designation}-${index}`}>
                  <td>{line.lot}</td>
                  <td>{line.famille || "-"}</td>
                  <td>{line.designation}</td>
                  <td>{line.unite || "-"}</td>
                  <td>{formatNumber(line.quantite)}</td>
                  <td>{formatCurrency(line.prixUnitaire, currencyCode)}</td>
                  <td>{formatCurrency(line.prixTotal, currencyCode)}</td>
                  <td>
                    <button className="ghost-button" type="button" onClick={() => dqeState.removeDraftLine(index)}>
                      Retirer
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export function DqeDocumentImportPanel({ dqeState, projectContext }) {
  return (
    <section className="panel import-panel">
      <div className="panel-heading">
        <div>
          <p className="panel-label">OCR</p>
          <h3>Importer un PDF ou une image DQE</h3>
        </div>
      </div>

      <div className="import-form">
        <FilterSelect
          label="Projet cible"
          value={dqeState.selectedProjectId || projectContext.activeProjectId}
          options={projectContext.projects.map((project) => ({
            label: project.name,
            value: String(project.id),
          }))}
          onChange={dqeState.setSelectedProjectId}
          placeholder="Choisir un projet"
        />

        <label className="filter-field">
          <span>Document source</span>
          <input
            className="file-input"
            type="file"
            accept=".pdf,.png,.jpg,.jpeg,.webp"
            onChange={(event) => dqeState.setDqeDocumentFile(event.target.files?.[0] ?? null)}
          />
        </label>

        <button
          className="primary-button"
          type="button"
          onClick={dqeState.handleDqeDocumentImport}
          disabled={dqeState.documentLoading}
        >
          {dqeState.documentLoading ? "Analyse en cours..." : "Analyser puis importer"}
        </button>
      </div>

      {dqeState.documentFile && (
        <p className="helper-text">Document selectionne : {dqeState.documentFile.name}</p>
      )}

      {dqeState.documentMessage && <p className="success-text">{dqeState.documentMessage}</p>}
      {dqeState.documentError && <p className="error-text">{dqeState.documentError}</p>}
    </section>
  );
}

export function ErrorPanel({ error, backendHint = "" }) {
  if (!error) {
    return null;
  }

  return (
    <section className="panel error-panel">
      <h3>Erreur de chargement</h3>
      <p>{error}</p>
      {backendHint && <p>{backendHint}</p>}
      <p>Verifie que le backend Spring Boot tourne bien sur {BACKEND_LABEL}.</p>
    </section>
  );
}
