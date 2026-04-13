// ===============================
// 1. CONFIGURATION DES FILTRES
// ===============================
// Cette structure permet de piloter la barre de filtres
// depuis une configuration unique.

export const SCENARIO_OPTIONS = [
  { value: "LOCAL", label: "Local" },
  { value: "IMPORT", label: "Import" },
  { value: "MIX", label: "Mix" },
];

export function buildGlobalFilterConfig({ filters, data, projectContext }) {
  return [
    {
      key: "project",
      label: "Projet",
      value: projectContext.activeProjectId,
      options: projectContext.projects.map((project) => ({
        label: project.name,
        value: String(project.id),
      })),
      onChange: projectContext.setActiveProjectId,
      placeholder: "Choisir un projet",
    },
    {
      key: "batiment",
      label: "Batiment",
      value: filters.batimentFilter,
      options: data.batimentOptions,
      onChange: filters.setBatimentFilter,
      placeholder: "Tous",
    },
    {
      key: "niveau",
      label: "Niveau",
      value: filters.niveauFilter,
      options: data.niveauOptions,
      onChange: filters.setNiveauFilter,
      placeholder: "Tous",
    },
    {
      key: "lot",
      label: "Lot",
      value: filters.lotFilter,
      options: data.lotOptions,
      onChange: filters.setLotFilter,
      placeholder: "Tous",
    },
    {
      key: "famille",
      label: "Famille article",
      value: filters.familleFilter,
      options: data.familleOptions,
      onChange: filters.setFamilleFilter,
      placeholder: "Tous",
    },
    {
      key: "scenario",
      label: "Scenario",
      value: filters.scenarioFilter,
      options: SCENARIO_OPTIONS,
      onChange: filters.setScenarioFilter,
      placeholder: "Tous",
    },
  ];
}
