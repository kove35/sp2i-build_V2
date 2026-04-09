import { useNavigate } from "react-router-dom";

/**
 * Landing page volontairement sobre.
 *
 * Le cahier des charges ici est different des dashboards :
 * - pas de sidebar
 * - un message clair
 * - une page qui tient visuellement sur un ecran
 */
export default function HomePremium() {
  const navigate = useNavigate();

  return (
    <div className="landing landing-compact">
      <section className="landing-compact-shell">
        <div className="landing-compact-copy">
          <p className="landing-kicker">SP2I BUILD</p>

          <h1>Pilotez vos projets de construction avec precision</h1>

          <p className="subtitle">
            Reduisez vos couts, securisez vos decisions et gardez une vision claire du chantier, du
            DQE jusqu&apos;au pilotage CAPEX.
          </p>

          <div className="actions">
            <button className="primary-button" type="button" onClick={() => navigate("/projects/create")}>
              Creer mon projet
            </button>
            <button className="ghost-button" type="button" onClick={() => navigate("/demo")}>
              Demarrer la demo
            </button>
          </div>
        </div>

        <div className="landing-compact-panel">
          <div className="landing-mini-metric">
            <span>Analyse CAPEX</span>
            <strong>Local vs Import</strong>
          </div>
          <div className="landing-mini-metric">
            <span>Import DQE</span>
            <strong>Excel, PDF, image</strong>
          </div>
          <div className="landing-mini-metric">
            <span>Vision chantier</span>
            <strong>Batiments et niveaux</strong>
          </div>
          <div className="landing-mini-metric">
            <span>Decision</span>
            <strong>Jusqu&apos;a 30% d&apos;economie</strong>
          </div>
        </div>
      </section>
    </div>
  );
}
