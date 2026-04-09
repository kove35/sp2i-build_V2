import {
  ErrorPanel,
  FilterPanel,
  LoadingPanel,
  PageHeader,
} from "../components/DashboardBits";
import { extractDayNumber, formatNumber } from "../lib/capex";

/**
 * Cette page affiche le planning chantier genere par le backend.
 *
 * Idee pedagogique :
 * - le backend transforme les CapexItem en taches ordonnees
 * - le frontend se contente de presenter ces taches clairement
 * - on garde donc la logique metier cote Spring Boot
 */
export default function Planning({ dashboard }) {
  const { filters, data, projectContext } = dashboard;
  const tasks = data.planningTasks ?? [];

  const totalDuration =
    tasks.length === 0 ? 0 : Math.max(...tasks.map((task) => extractDayNumber(task.dateFin)));
  const batimentCount = new Set(tasks.map((task) => task.batiment).filter(Boolean)).size;
  const niveauCount = new Set(tasks.map((task) => task.niveau).filter(Boolean)).size;

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Planning"
        title="Planning chantier intelligent"
        description="Une vue simple pour voir l'ordre des lots, la duree estimee et la chronologie cumulative du chantier projet par projet."
      />

      <FilterPanel filters={filters} data={data} projectContext={projectContext} />

      {(data.loading || data.catalogLoading || data.planningLoading) && (
        <LoadingPanel message="Chargement du planning chantier..." />
      )}

      {!data.loading && !data.catalogLoading && !data.planningLoading && (
        <>
          <ErrorPanel error={data.error || data.planningError} />

          {!data.error && !data.planningError && (
            <>
              <section className="three-grid">
                <article className="panel insight-card">
                  <p className="panel-label">Planning</p>
                  <h3>Taches planifiees</h3>
                  <strong>{formatNumber(tasks.length)}</strong>
                  <p className="helper-text">Nombre de taches chantier generees pour le projet actif.</p>
                </article>

                <article className="panel insight-card">
                  <p className="panel-label">Planning</p>
                  <h3>Duree totale</h3>
                  <strong>{formatNumber(totalDuration)} jours</strong>
                  <p className="helper-text">Le calcul cumule les durees dans l'ordre d'execution.</p>
                </article>

                <article className="panel insight-card">
                  <p className="panel-label">Couverture</p>
                  <h3>Zones couvertes</h3>
                  <strong>
                    {formatNumber(batimentCount)} bat. / {formatNumber(niveauCount)} niv.
                  </strong>
                  <p className="helper-text">Lecture rapide des batiments et niveaux touches par le chantier.</p>
                </article>
              </section>

              <section className="content-grid">
                <article className="panel">
                  <div className="panel-heading">
                    <div>
                      <p className="panel-label">Timeline</p>
                      <h3>Sequence des interventions</h3>
                    </div>
                  </div>

                  {tasks.length === 0 ? (
                    <div className="empty-state">Aucune tache disponible pour ce projet.</div>
                  ) : (
                    <div className="planning-timeline">
                      {tasks.map((task, index) => (
                        <div className="planning-row" key={`${task.lot}-${task.batiment}-${task.niveau}-${index}`}>
                          <div className="planning-order">{index + 1}</div>

                          <div className="planning-details">
                            <div className="planning-main">
                              <strong>{task.lot || "Lot non renseigne"}</strong>
                              <span>
                                {task.batiment || "Batiment non renseigne"} | {task.niveau || "Niveau non renseigne"}
                              </span>
                            </div>

                            <div className="planning-bar-track">
                              <div
                                className="planning-bar-fill"
                                style={{
                                  left: `${extractDayNumber(task.dateDebut) * 2.4}%`,
                                  width: `${Math.max(task.duree ?? 0, 1) * 2.4}%`,
                                }}
                              />
                            </div>
                          </div>

                          <div className="planning-dates">
                            <strong>{task.duree ?? 0} j</strong>
                            <span>
                              {task.dateDebut} {"->"} {task.dateFin}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </article>

                <article className="panel side-panel">
                  <p className="panel-label">Lecture rapide</p>
                  <h3>Resume des taches</h3>

                  <div className="stack-list">
                    {tasks.length === 0 && <div className="empty-state">Aucune tache chantier a resumer.</div>}

                    {tasks.map((task, index) => (
                      <div
                        className="stack-row stack-row-static"
                        key={`${task.lot}-${task.batiment}-${task.niveau}-summary-${index}`}
                      >
                        <span>
                          {index + 1}. {task.lot || "Lot"}
                        </span>
                        <strong>{task.dateFin}</strong>
                      </div>
                    ))}
                  </div>
                </article>
              </section>

              <section className="panel table-panel">
                <div className="panel-heading">
                  <div>
                    <p className="panel-label">Detail</p>
                    <h3>Tableau de planning</h3>
                  </div>
                  <span className="table-count">{tasks.length} tache(s)</span>
                </div>

                {tasks.length === 0 ? (
                  <div className="empty-state">Le backend ne retourne encore aucune tache pour ce projet.</div>
                ) : (
                  <div className="table-wrapper">
                    <table>
                      <thead>
                        <tr>
                          <th>Ordre</th>
                          <th>Lot</th>
                          <th>Batiment</th>
                          <th>Niveau</th>
                          <th>Debut</th>
                          <th>Fin</th>
                          <th>Duree</th>
                        </tr>
                      </thead>
                      <tbody>
                        {tasks.map((task, index) => (
                          <tr key={`${task.lot}-${task.batiment}-${task.niveau}-table-${index}`}>
                            <td>{index + 1}</td>
                            <td>{task.lot || "-"}</td>
                            <td>{task.batiment || "-"}</td>
                            <td>{task.niveau || "-"}</td>
                            <td>{task.dateDebut || "-"}</td>
                            <td>{task.dateFin || "-"}</td>
                            <td>{task.duree ?? 0} jours</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            </>
          )}
        </>
      )}
    </div>
  );
}
