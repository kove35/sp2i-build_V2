import { useNavigate } from "react-router-dom";
import { PageHeader } from "../components/DashboardBits";

/**
 * Page dediee a la creation complete d'un projet.
 *
 * On regroupe ici :
 * - les informations generales
 * - la logistique
 * - les regles metier
 * - la structure immobiliere
 *
 * Le formulaire s'appuie sur l'etat global du hook dashboard.
 */
export default function CreateProject({ dashboard }) {
  const navigate = useNavigate();
  const { auth, projectContext } = dashboard;

  async function handleSubmit(event) {
    event.preventDefault();
    const createdProject = await auth.createProject(buildPayload(projectContext.projectForm));
    if (createdProject) {
      navigate("/direction");
    }
  }

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Projet"
        title="Creer un projet SP2I Build"
        description="Renseigne une fiche projet complete avec les hypotheses logistiques, la strategie import et la structure immobiliere pour preparer un pilotage CAPEX propre."
      />

      <form className="create-project-layout" onSubmit={handleSubmit}>
        <section className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Infos projet</p>
              <h3>Carte d'identite</h3>
            </div>
          </div>

          <div className="filters-grid three-field-grid">
            <ProjectInput
              label="Nom du projet"
              value={projectContext.projectForm.name}
              onChange={(value) => projectContext.updateField("name", value)}
              placeholder="Ex: Extension site Pointe-Noire"
            />
            <ProjectInput
              label="Localisation"
              value={projectContext.projectForm.location}
              onChange={(value) => projectContext.updateField("location", value)}
              placeholder="Ex: Pointe-Noire"
            />
            <ProjectInput
              label="Type"
              value={projectContext.projectForm.type}
              onChange={(value) => projectContext.updateField("type", value)}
              placeholder="Ex: Industriel"
            />
            <ProjectInput
              label="Surface (m2)"
              type="number"
              value={projectContext.projectForm.surface}
              onChange={(value) => projectContext.updateField("surface", value)}
              placeholder="12000"
            />
            <ProjectInput
              label={`Budget (${getCurrencyLabel(projectContext.projectForm.currencyCode)})`}
              type="number"
              value={projectContext.projectForm.budget}
              onChange={(value) => projectContext.updateField("budget", value)}
              placeholder="4500000"
            />
            <label className="filter-field">
              <span>Devise projet</span>
              <select
                value={projectContext.projectForm.currencyCode}
                onChange={(event) => projectContext.updateField("currencyCode", event.target.value)}
              >
                <option value="XAF">Franc CFA (XAF)</option>
                <option value="EUR">Euro (EUR)</option>
                <option value="USD">Dollar US (USD)</option>
              </select>
            </label>
          </div>
        </section>

        <section className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Logistique</p>
              <h3>Hypotheses import</h3>
            </div>
          </div>

          <div className="slider-grid">
            <ProjectSlider
              label="Transport"
              value={projectContext.projectForm.transportRate}
              onChange={(value) => projectContext.updateField("transportRate", value)}
            />
            <ProjectSlider
              label="Douane"
              value={projectContext.projectForm.douaneRate}
              onChange={(value) => projectContext.updateField("douaneRate", value)}
            />
            <ProjectSlider
              label="Port"
              value={projectContext.projectForm.portRate}
              onChange={(value) => projectContext.updateField("portRate", value)}
            />
            <ProjectSlider
              label="Transport local"
              value={projectContext.projectForm.localRate}
              onChange={(value) => projectContext.updateField("localRate", value)}
            />
            <ProjectSlider
              label="Marge"
              value={projectContext.projectForm.marginRate}
              onChange={(value) => projectContext.updateField("marginRate", value)}
            />
            <ProjectSlider
              label="Risque"
              value={projectContext.projectForm.riskRate}
              onChange={(value) => projectContext.updateField("riskRate", value)}
            />
          </div>
        </section>

        <section className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Metier</p>
              <h3>Strategie CAPEX</h3>
            </div>
          </div>

          <div className="filters-grid three-field-grid">
            <ProjectSlider
              label="Seuil import"
              value={projectContext.projectForm.importThreshold}
              onChange={(value) => projectContext.updateField("importThreshold", value)}
              max={100}
            />
            <label className="filter-field">
              <span>Mode strategique</span>
              <select
                value={projectContext.projectForm.strategyMode}
                onChange={(event) => projectContext.updateField("strategyMode", event.target.value)}
              >
                <option value="LOCAL_FIRST">LOCAL_FIRST</option>
                <option value="HYBRID">HYBRID</option>
                <option value="IMPORT_FIRST">IMPORT_FIRST</option>
              </select>
            </label>
          </div>
        </section>

        <section className="panel">
          <div className="panel-heading">
            <div>
              <p className="panel-label">Structure</p>
              <h3>Organisation immobiliere</h3>
            </div>
            <button className="ghost-button" type="button" onClick={projectContext.addBuilding}>
              Ajouter un batiment
            </button>
          </div>

          <div className="building-stack">
            {projectContext.projectForm.batiments.map((building, index) => (
              <article className="building-card" key={`building-${index}`}>
                <div className="building-header">
                  <strong>Batiment {index + 1}</strong>
                  {projectContext.projectForm.batiments.length > 1 && (
                    <button
                      className="ghost-button"
                      type="button"
                      onClick={() => projectContext.removeBuilding(index)}
                    >
                      Retirer
                    </button>
                  )}
                </div>

                <div className="filters-grid three-field-grid">
                  <ProjectInput
                    label="Nom"
                    value={building.nom}
                    onChange={(value) => projectContext.updateBuilding(index, "nom", value)}
                    placeholder="Ex: Batiment A"
                  />
                  <ProjectInput
                    label="Etages"
                    value={building.etagesText}
                    onChange={(value) => projectContext.updateBuilding(index, "etagesText", value)}
                    placeholder="RDC, R+1, R+2"
                  />
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className="panel create-project-footer">
          <div>
            <p className="panel-label">Validation</p>
            <h3>Creer le projet</h3>
            <p className="helper-text">
              Le projet sera enregistre, rattache a ton utilisateur et disponible dans les dashboards.
            </p>
          </div>

          <div className="builder-actions">
            <button className="ghost-button" type="button" onClick={() => navigate("/direction")}>
              Annuler
            </button>
            <button className="primary-button" type="submit" disabled={projectContext.projectCreationLoading}>
              {projectContext.projectCreationLoading ? "Creation..." : "Creer le projet"}
            </button>
          </div>
        </section>

        {projectContext.projectCreationError && (
          <section className="panel error-panel">
            <h3>Erreur de creation</h3>
            <p>{projectContext.projectCreationError}</p>
          </section>
        )}
      </form>
    </div>
  );
}

function ProjectInput({ label, value, onChange, placeholder, type = "text" }) {
  return (
    <label className="filter-field">
      <span>{label}</span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
      />
    </label>
  );
}

function ProjectSlider({ label, value, onChange, max = 30 }) {
  return (
    <label className="slider-field">
      <div className="slider-label-row">
        <span>{label}</span>
        <strong>{value}%</strong>
      </div>
      <input
        type="range"
        min="0"
        max={max}
        step="1"
        value={value}
        onChange={(event) => onChange(Number(event.target.value))}
      />
    </label>
  );
}

function buildPayload(form) {
  return {
    name: form.name,
    location: form.location,
    type: form.type,
    surface: parseOptionalNumber(form.surface),
    budget: parseOptionalNumber(form.budget),
    currencyCode: form.currencyCode || "XAF",
    transportRate: percentToDecimal(form.transportRate),
    douaneRate: percentToDecimal(form.douaneRate),
    portRate: percentToDecimal(form.portRate),
    localRate: percentToDecimal(form.localRate),
    marginRate: percentToDecimal(form.marginRate),
    riskRate: percentToDecimal(form.riskRate),
    importThreshold: percentToDecimal(form.importThreshold),
    strategyMode: form.strategyMode,
    structure: {
      batiments: form.batiments
        .filter((building) => building.nom.trim())
        .map((building) => ({
          nom: building.nom.trim(),
          etages: building.etagesText
            .split(",")
            .map((floor) => floor.trim())
            .filter(Boolean),
        })),
    },
  };
}

function parseOptionalNumber(value) {
  if (value === "" || value == null) {
    return null;
  }
  return Number(value);
}

function percentToDecimal(value) {
  return Number(value) / 100;
}

function getCurrencyLabel(currencyCode) {
  if (currencyCode === "EUR") {
    return "EUR";
  }
  if (currencyCode === "USD") {
    return "USD";
  }
  return "FCFA";
}
