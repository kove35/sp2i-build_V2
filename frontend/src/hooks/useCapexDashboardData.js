import { useEffect, useMemo, useState } from "react";
import {
  API_URL,
  buildBatimentNiveauHeatmap,
  buildCoverageMetrics,
  buildChartData,
  buildFamilyEntries,
  buildGroupedEntries,
  buildZoneEntries,
  normalizeFamilyLabel,
  normalizeLotLabel,
  normalizeZoneLabel,
  buildQueryString,
  buildTopEconomies,
  extractOptions,
} from "../lib/capex";

const ACTIVE_PROJECT_STORAGE_KEY = "sp2i-active-project-id";
const AUTH_TOKEN_STORAGE_KEY = "sp2i-auth-token";
const AUTH_EMAIL_STORAGE_KEY = "sp2i-auth-email";
const AUTH_USER_ID_STORAGE_KEY = "sp2i-auth-user-id";
const DEMO_DQE_FILE_URL = "/demo/DQE_MEDICAL_CENTER_13-08-2025.pdf";
const RENDER_RETRYABLE_STATUSES = new Set([502, 503, 504]);
const RENDER_COLD_START_RETRIES = 2;
const RENDER_COLD_START_DELAY_MS = 2500;
const LOT_LABELS_BY_CODE = {
  "1": "Gros oeuvre",
  "2": "Etancheite",
  "3": "Revetements durs",
  "4": "Menuiserie aluminium et vitrerie",
  "5": "Menuiserie metallique et ferronnerie",
  "6": "Menuiserie bois",
  "7": "Electricite",
  "8": "Climatisation",
  "9": "Securite incendie et video surveillance",
  "10": "Plomberie sanitaire",
  "11": "Faux plafond et cloisons BA13",
  "12": "Ascenseur",
  "13": "Alucobond",
  "14": "Peinture",
};

const EMPTY_DQE_LINE = {
  lot: "",
  famille: "",
  designation: "",
  unite: "U",
  quantite: "",
  prixUnitaire: "",
};

function createDefaultProjectForm() {
  return {
    name: "",
    location: "",
    type: "Industriel",
    surface: "",
    budget: "",
    currencyCode: "XAF",
    transportRate: 8,
    douaneRate: 15,
    portRate: 5,
    localRate: 5,
    marginRate: 10,
    riskRate: 5,
    importThreshold: 20,
    strategyMode: "HYBRID",
    batiments: [
      {
        nom: "",
        etagesText: "",
      },
    ],
  };
}

/**
 * Hook principal du cockpit SP2I Build.
 *
 * Il centralise :
 * - les filtres BI
 * - le projet actif
 * - les chargements de donnees
 * - les imports DQE
 * - le builder DQE manuel
 *
 * Les pages React restent ainsi concentrees sur l'affichage.
 */
export function useCapexDashboardData() {
  const [lotFilter, setLotFilter] = useState("");
  const [familleFilter, setFamilleFilter] = useState("");
  const [batimentFilter, setBatimentFilter] = useState("");
  const [niveauFilter, setNiveauFilter] = useState("");

  const [summary, setSummary] = useState(null);
  const [items, setItems] = useState([]);
  const [allItems, setAllItems] = useState([]);
  const [recentItems, setRecentItems] = useState([]);
  const [planningTasks, setPlanningTasks] = useState([]);
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [planningLoading, setPlanningLoading] = useState(false);
  const [error, setError] = useState("");
  const [planningError, setPlanningError] = useState("");
  const [backendWakeInProgress, setBackendWakeInProgress] = useState(false);
  const [backendStatusMessage, setBackendStatusMessage] = useState("");

  const [activeProjectId, setActiveProjectId] = useState(() => {
    return window.localStorage.getItem(ACTIVE_PROJECT_STORAGE_KEY) ?? "";
  });
  const [authToken, setAuthToken] = useState(() => window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY) ?? "");
  const [authEmail, setAuthEmail] = useState(() => window.localStorage.getItem(AUTH_EMAIL_STORAGE_KEY) ?? "");
  const [authUserId, setAuthUserId] = useState(() => window.localStorage.getItem(AUTH_USER_ID_STORAGE_KEY) ?? "");
  const [authLoading, setAuthLoading] = useState(false);
  const [authError, setAuthError] = useState("");
  const [projectCreationName, setProjectCreationName] = useState("");
  const [projectCreationLoading, setProjectCreationLoading] = useState(false);
  const [projectCreationError, setProjectCreationError] = useState("");
  const [projectForm, setProjectForm] = useState(() => createDefaultProjectForm());

  const [selectedProjectId, setSelectedProjectId] = useState("");
  const [selectedFile, setSelectedFile] = useState(null);
  const [importLoading, setImportLoading] = useState(false);
  const [importMessage, setImportMessage] = useState("");
  const [importError, setImportError] = useState("");

  const [dqeDraft, setDqeDraft] = useState(EMPTY_DQE_LINE);
  const [dqeDraftLines, setDqeDraftLines] = useState([]);
  const [dqeBuilderLoading, setDqeBuilderLoading] = useState(false);
  const [dqeBuilderMessage, setDqeBuilderMessage] = useState("");
  const [dqeBuilderError, setDqeBuilderError] = useState("");
  const [dqeFamilySuggestions, setDqeFamilySuggestions] = useState([]);
  const [dqeDocumentFile, setDqeDocumentFile] = useState(null);
  const [dqeDocumentLoading, setDqeDocumentLoading] = useState(false);
  const [dqeDocumentMessage, setDqeDocumentMessage] = useState("");
  const [dqeDocumentError, setDqeDocumentError] = useState("");
  const [dqeAnalysisFile, setDqeAnalysisFile] = useState(null);
  const [dqeAnalysisLoading, setDqeAnalysisLoading] = useState(false);
  const [dqeAnalysisError, setDqeAnalysisError] = useState("");
  const [dqeAnalysisResult, setDqeAnalysisResult] = useState(null);
  const [dqeAnalysisImportLoading, setDqeAnalysisImportLoading] = useState(false);
  const [dqeAnalysisImportMessage, setDqeAnalysisImportMessage] = useState("");
  const [dqeAiFile, setDqeAiFile] = useState(null);
  const [dqeAiLoading, setDqeAiLoading] = useState(false);
  const [dqeAiError, setDqeAiError] = useState("");
  const [dqeAiResult, setDqeAiResult] = useState(null);
  const [dqeAiImportLoading, setDqeAiImportLoading] = useState(false);
  const [dqeAiImportMessage, setDqeAiImportMessage] = useState("");
  const [dqeFullFile, setDqeFullFile] = useState(null);
  const [dqeStructuredText, setDqeStructuredText] = useState("");
  const [dqeFullLoading, setDqeFullLoading] = useState(false);
  const [dqeFullError, setDqeFullError] = useState("");
  const [dqeFullResult, setDqeFullResult] = useState(null);
  const [dqeFullEditableLines, setDqeFullEditableLines] = useState([]);
  const [dqeFullImportLoading, setDqeFullImportLoading] = useState(false);
  const [dqeFullImportMessage, setDqeFullImportMessage] = useState("");
  const [dqeFullProgress, setDqeFullProgress] = useState(0);

  useEffect(() => {
    setDqeFullEditableLines(dqeFullResult?.lignes ? dqeFullResult.lignes.map((line) => ({ ...line })) : []);
  }, [dqeFullResult]);

  useEffect(() => {
    window.localStorage.setItem(ACTIVE_PROJECT_STORAGE_KEY, activeProjectId);
    if (!selectedProjectId && activeProjectId) {
      setSelectedProjectId(activeProjectId);
    }
  }, [activeProjectId, selectedProjectId]);

  useEffect(() => {
    loadProjects();
  }, [authToken]);

  useEffect(() => {
    if (activeProjectId) {
      loadCatalogForProject();
      loadPlanning();
    } else {
      setAllItems([]);
      setRecentItems([]);
      setItems([]);
      setSummary(null);
      setPlanningTasks([]);
      setCatalogLoading(false);
      setLoading(false);
      setPlanningLoading(false);
    }
  }, [activeProjectId]);

  useEffect(() => {
    if (activeProjectId) {
      loadDashboard();
    }
  }, [lotFilter, familleFilter, batimentFilter, niveauFilter, activeProjectId]);

  useEffect(() => {
    loadFamilySuggestions(dqeDraft.lot);
  }, [dqeDraft.lot]);

  const dqePrixTotal = useMemo(() => {
    return normalizeNumericValue(dqeDraft.quantite) * normalizeNumericValue(dqeDraft.prixUnitaire);
  }, [dqeDraft.prixUnitaire, dqeDraft.quantite]);

  const activeProject = useMemo(() => {
    return projects.find((project) => String(project.id) === activeProjectId) ?? null;
  }, [projects, activeProjectId]);

  const activeCurrencyCode = activeProject?.currencyCode || "XAF";

  async function loadProjects() {
    try {
      const response = await apiFetch(`/projects`, {
        headers: buildAuthHeaders(),
      });

      if (!response.ok) {
        throw new Error("Impossible de charger la liste des projets.");
      }

      const projectsData = await response.json();
      setProjects(projectsData);

      if (projectsData.length > 0) {
        const storedProjectStillExists = projectsData.some(
          (project) => String(project.id) === activeProjectId
        );

        if (!storedProjectStillExists) {
          const firstProjectId = String(projectsData[0].id);
          setActiveProjectId(firstProjectId);
          setSelectedProjectId(firstProjectId);
        }
      }
    } catch (loadProjectsError) {
      setError(loadProjectsError.message);
    }
  }

  async function loadCatalogForProject() {
    try {
      setCatalogLoading(true);
      setError("");

      const projectQuery = buildQueryString({ projectId: activeProjectId });

      const [itemsResponse, recentItemsResponse] = await Promise.all([
        apiFetch(`/capex/items${projectQuery}`, {
          headers: buildAuthHeaders(),
        }),
        apiFetch(`/capex/items/recent${projectQuery}`, {
          headers: buildAuthHeaders(),
        }),
      ]);

      if (!itemsResponse.ok) {
        throw new Error("Impossible de charger les valeurs de filtres.");
      }

      if (!recentItemsResponse.ok) {
        throw new Error("Impossible de charger les derniers items CAPEX.");
      }

      const [itemsData, recentItemsData] = await Promise.all([
        itemsResponse.json(),
        recentItemsResponse.json(),
      ]);

      setAllItems(itemsData);
      setRecentItems(recentItemsData);
    } catch (catalogError) {
      setError(catalogError.message);
    } finally {
      setCatalogLoading(false);
    }
  }

  async function loadDashboard() {
    try {
      setLoading(true);
      setError("");

      const queryString = buildQueryString({
        lot: lotFilter,
        famille: familleFilter,
        batiment: batimentFilter,
        niveau: niveauFilter,
        projectId: activeProjectId,
      });

      const [summaryResponse, itemsResponse] = await Promise.all([
        apiFetch(`/capex/summary${queryString}`, {
          headers: buildAuthHeaders(),
        }),
        apiFetch(`/capex/items${queryString}`, {
          headers: buildAuthHeaders(),
        }),
      ]);

      if (!summaryResponse.ok) {
        throw new Error("Impossible de charger le resume CAPEX.");
      }

      if (!itemsResponse.ok) {
        throw new Error("Impossible de charger la liste des items CAPEX.");
      }

      const [summaryData, itemsData] = await Promise.all([
        summaryResponse.json(),
        itemsResponse.json(),
      ]);

      setSummary(summaryData);
      setItems(itemsData);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  }

  /**
   * Charge le planning chantier simplifie genere par le backend.
   *
   * Cette vue est separee du resume CAPEX classique :
   * - elle ne calcule pas des KPI financiers
   * - elle recupere une liste de taches deja ordonnees
   */
  async function loadPlanning() {
    if (!activeProjectId) {
      setPlanningTasks([]);
      return;
    }

    try {
      setPlanningLoading(true);
      setPlanningError("");

      const response = await apiFetch(`/planning/${activeProjectId}`, {
        headers: buildAuthHeaders(),
      });

      if (!response.ok) {
        throw new Error("Impossible de charger le planning chantier.");
      }

      const planningData = await response.json();
      setPlanningTasks(planningData);
    } catch (loadPlanningError) {
      setPlanningError(loadPlanningError.message);
    } finally {
      setPlanningLoading(false);
    }
  }

  async function loadFamilySuggestions(lot) {
    if (!lot) {
      setDqeFamilySuggestions([]);
      return;
    }

    try {
      const queryString = buildQueryString({ lot });
      const response = await apiFetch(`/dqe/families/suggestions${queryString}`, {
        headers: buildAuthHeaders(),
      });

      if (!response.ok) {
        throw new Error("Impossible de charger les suggestions de familles.");
      }

      const payload = await response.json();
      setDqeFamilySuggestions(payload.familles ?? []);
    } catch (suggestionsError) {
      setDqeBuilderError(suggestionsError.message);
    }
  }

  async function refreshAllData() {
    await Promise.all([loadCatalogForProject(), loadDashboard(), loadPlanning()]);
  }

  async function ensureTargetProjectIdForDqeImport(projectName = "Projet DQE SP2I") {
    if (activeProjectId) {
      return activeProjectId;
    }

    if (projects.length > 0) {
      const fallbackProjectId = String(projects[0].id);
      setActiveProjectId(fallbackProjectId);
      setSelectedProjectId(fallbackProjectId);
      return fallbackProjectId;
    }

    const createdProject = await createProject({ name: projectName });

    if (!createdProject?.id) {
      throw new Error("Impossible de creer automatiquement un projet cible pour l'import.");
    }

    return String(createdProject.id);
  }

  async function handleImport() {
    const targetProjectId = selectedProjectId || activeProjectId;

    if (!targetProjectId) {
      setImportError("Choisis d'abord un projet cible.");
      setImportMessage("");
      return;
    }

    if (!selectedFile) {
      setImportError("Choisis un fichier Excel a importer.");
      setImportMessage("");
      return;
    }

    try {
      setImportLoading(true);
      setImportError("");
      setImportMessage("");

      const formData = new FormData();
      formData.append("projectId", targetProjectId);
      formData.append("file", selectedFile);

      const response = await apiFetch(`/capex/import`, {
        method: "POST",
        headers: buildAuthHeaders(false),
        body: formData,
      });

      const responseBody = await response.json();

      if (!response.ok) {
        throw new Error(responseBody.message || "Erreur pendant l'import du fichier.");
      }

      setImportMessage(`${responseBody.lignesImportees} ligne(s) importee(s) avec succes.`);
      setSelectedFile(null);
      setActiveProjectId(String(targetProjectId));
      await refreshAllData();
    } catch (uploadError) {
      setImportError(uploadError.message);
    } finally {
      setImportLoading(false);
    }
  }

  async function handleTemplateDownload() {
    try {
      setImportError("");

      const response = await apiFetch(`/capex/import/template`, {
        headers: buildAuthHeaders(),
      });

      if (!response.ok) {
        throw new Error("Impossible de telecharger le modele Excel.");
      }

      const blob = await response.blob();
      triggerBrowserDownload(blob, "dqe-import-template.xlsx");
    } catch (downloadError) {
      setImportError(downloadError.message);
    }
  }

  function clearFilters() {
    setLotFilter("");
    setFamilleFilter("");
    setBatimentFilter("");
    setNiveauFilter("");
  }

  function updateDqeDraft(fieldName, value) {
    setDqeBuilderError("");
    setDqeBuilderMessage("");
    setDqeDraft((currentDraft) => ({
      ...currentDraft,
      [fieldName]: value,
    }));
  }

  function addDqeDraftLine() {
    const validationError = validateDqeDraftLine(dqeDraft);
    if (validationError) {
      setDqeBuilderError(validationError);
      return;
    }

    setDqeDraftLines((currentLines) => [
      ...currentLines,
      {
        ...dqeDraft,
        quantite: normalizeNumericValue(dqeDraft.quantite),
        prixUnitaire: normalizeNumericValue(dqeDraft.prixUnitaire),
        prixTotal: dqePrixTotal,
      },
    ]);
    setDqeDraft(EMPTY_DQE_LINE);
    setDqeBuilderError("");
  }

  function removeDqeDraftLine(indexToRemove) {
    setDqeDraftLines((currentLines) =>
      currentLines.filter((_, lineIndex) => lineIndex !== indexToRemove)
    );
  }

  async function saveDqeDraftLines() {
    const targetProjectId = selectedProjectId || activeProjectId;

    if (!targetProjectId) {
      setDqeBuilderError("Choisis un projet cible avant d'enregistrer le DQE.");
      return;
    }

    if (dqeDraftLines.length === 0) {
      setDqeBuilderError("Ajoute au moins une ligne DQE avant l'enregistrement.");
      return;
    }

    try {
      setDqeBuilderLoading(true);
      setDqeBuilderError("");
      setDqeBuilderMessage("");

      for (const line of dqeDraftLines) {
        const response = await apiFetch(`/dqe/items`, {
          method: "POST",
          headers: buildAuthHeaders(),
          body: JSON.stringify({
            projectId: Number(targetProjectId),
            lot: line.lot,
            famille: line.famille,
            designation: line.designation,
            unite: line.unite,
            quantite: line.quantite,
            prixUnitaire: line.prixUnitaire,
          }),
        });

        const payload = await response.json();

        if (!response.ok) {
          throw new Error(payload.message || "Impossible d'enregistrer une ligne DQE.");
        }
      }

      setDqeBuilderMessage(`${dqeDraftLines.length} ligne(s) DQE enregistree(s) avec succes.`);
      setDqeDraftLines([]);
      setActiveProjectId(String(targetProjectId));
      await refreshAllData();
    } catch (saveError) {
      setDqeBuilderError(saveError.message);
    } finally {
      setDqeBuilderLoading(false);
    }
  }

  async function handleDqeExport() {
    const targetProjectId = selectedProjectId || activeProjectId;

    if (!targetProjectId) {
      setDqeBuilderError("Choisis un projet avant l'export Excel.");
      return;
    }

    try {
      setDqeBuilderError("");
      const queryString = buildQueryString({ projectId: targetProjectId });
      const response = await apiFetch(`/dqe/export${queryString}`, {
        headers: buildAuthHeaders(),
      });

      if (!response.ok) {
        throw new Error("Impossible de telecharger l'export DQE.");
      }

      const blob = await response.blob();
      triggerBrowserDownload(blob, "dqe-builder-export.xlsx");
    } catch (exportError) {
      setDqeBuilderError(exportError.message);
    }
  }

  async function handleDqeDocumentImport() {
    const targetProjectId = selectedProjectId || activeProjectId;

    if (!targetProjectId) {
      setDqeDocumentError("Choisis un projet cible avant l'import PDF/Image.");
      return;
    }

    if (!dqeDocumentFile) {
      setDqeDocumentError("Choisis un PDF ou une image DQE.");
      return;
    }

    try {
      setDqeDocumentLoading(true);
      setDqeDocumentError("");
      setDqeDocumentMessage("");

      const formData = new FormData();
      formData.append("projectId", targetProjectId);
      formData.append("file", dqeDocumentFile);

      const response = await apiFetch(`/dqe/import`, {
        method: "POST",
        headers: buildAuthHeaders(false),
        body: formData,
      });

      const payload = await response.json();

      if (!response.ok) {
        throw new Error(payload.message || "Impossible d'importer le document DQE.");
      }

      const errorSuffix =
        payload.erreurs && payload.erreurs.length > 0
          ? ` ${payload.erreurs.length} erreur(s) a relire.`
          : "";

      setDqeDocumentMessage(`${payload.lignesImportees} ligne(s) reconnue(s) puis importee(s).${errorSuffix}`);
      setDqeDocumentFile(null);
      setActiveProjectId(String(targetProjectId));
      await refreshAllData();
    } catch (documentError) {
      setDqeDocumentError(documentError.message);
    } finally {
      setDqeDocumentLoading(false);
    }
  }

  /**
   * Lance une analyse DQE sans insertion en base.
   *
   * Cette operation est ideale pour la demo produit :
   * on montre ce que l'application comprend avant meme l'import.
   */
  async function handleDqeAnalysis() {
    if (!dqeAnalysisFile) {
      setDqeAnalysisError("Choisis un PDF ou une image a analyser.");
      return;
    }

    try {
      setDqeAnalysisLoading(true);
      setDqeAnalysisError("");
      setDqeAnalysisResult(null);
      setDqeAnalysisImportMessage("");

      const formData = new FormData();
      formData.append("file", dqeAnalysisFile);

      const response = await apiFetch(`/dqe/analyze`, {
        method: "POST",
        headers: buildAuthHeaders(false),
        body: formData,
      });

      const payload = await response.json();

      if (!response.ok) {
        throw new Error(payload.message || "Impossible d'analyser le document DQE.");
      }

      setDqeAnalysisResult(payload);
    } catch (analysisError) {
      setDqeAnalysisError(analysisError.message);
    } finally {
      setDqeAnalysisLoading(false);
    }
  }

  /**
   * Analyse un fichier reel embarque dans la demo.
   *
   * Cela permet de montrer rapidement la valeur du produit
   * sans demander a l'utilisateur de preparer un document.
   */
  async function handleRealDemoAnalysis() {
    try {
      setDqeAnalysisLoading(true);
      setDqeAnalysisError("");
      setDqeAnalysisResult(null);
      setDqeAnalysisImportMessage("");

      const demoFileResponse = await fetch(DEMO_DQE_FILE_URL);
      if (!demoFileResponse.ok) {
        throw new Error("Impossible de charger le fichier reel de demonstration.");
      }

      const demoBlob = await demoFileResponse.blob();
      const demoFile = new File([demoBlob], "BE_MONOPROJET_V2_0_05_03.xlsx", {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });

      setDqeAnalysisFile(demoFile);

      const formData = new FormData();
      formData.append("file", demoFile);

      const response = await apiFetch(`/dqe/analyze`, {
        method: "POST",
        headers: buildAuthHeaders(false),
        body: formData,
      });

      const payload = await response.json();

      if (!response.ok) {
        throw new Error(payload.message || "Impossible d'analyser l'exemple reel.");
      }

      setDqeAnalysisResult(payload);
    } catch (analysisError) {
      setDqeAnalysisError(analysisError.message);
    } finally {
      setDqeAnalysisLoading(false);
    }
  }

  /**
   * Importe dans le projet actif le document qui vient d'etre analyse.
   *
   * On reutilise volontairement l'endpoint d'import existant pour :
   * - eviter la duplication
   * - garder le meme pipeline backend
   * - transformer la demo en vrai cas d'usage operationnel
   */
  async function handleImportAnalyzedFile() {
    if (!dqeAnalysisFile) {
      setDqeAnalysisError("Aucun document analyse n'est disponible a importer.");
      return;
    }

    try {
      const targetProjectId = await ensureTargetProjectIdForDqeImport("Projet DQE analyse");
      setDqeAnalysisImportLoading(true);
      setDqeAnalysisError("");
      setDqeAnalysisImportMessage("");

      const formData = new FormData();
      formData.append("projectId", targetProjectId);
      formData.append("file", dqeAnalysisFile);

      const response = await apiFetch(`/dqe/import`, {
        method: "POST",
        headers: buildAuthHeaders(false),
        body: formData,
      });

      const payload = await response.json();

      if (!response.ok) {
        throw new Error(payload.message || "Impossible d'importer le document analyse.");
      }

      const errorSuffix =
        payload.erreurs && payload.erreurs.length > 0
          ? ` ${payload.erreurs.length} erreur(s) restent a verifier.`
          : "";

      setDqeAnalysisImportMessage(
        `${payload.lignesImportees} ligne(s) importee(s) dans le projet actif.${errorSuffix}`
      );

      await refreshAllData();
    } catch (importError) {
      setDqeAnalysisError(importError.message);
    } finally {
      setDqeAnalysisImportLoading(false);
    }
  }

  /**
   * Lance l'analyse IA enrichie pour la page de demo premium.
   *
   * On appelle ici le nouvel endpoint backend qui :
   * - estime les prix
   * - classe les lignes
   * - calcule un score de confiance
   * - propose une decision d'achat
   */
  async function handleDqeAiAnalysis() {
    if (!dqeAiFile) {
      setDqeAiError("Choisis un document PDF, image ou Excel a analyser.");
      return;
    }

    try {
      setDqeAiLoading(true);
      setDqeAiError("");
      setDqeAiResult(null);
      setDqeAiImportMessage("");

      const formData = new FormData();
      formData.append("file", dqeAiFile);

      const response = await apiFetch(`/dqe/analyze-ai`, {
        method: "POST",
        headers: buildAuthHeaders(false),
        body: formData,
      });

      const payload = await response.json();

      if (!response.ok) {
        throw new Error(payload.message || "Impossible d'analyser ce document avec le moteur AI.");
      }

      setDqeAiResult(payload);
    } catch (aiError) {
      setDqeAiError(aiError.message);
    } finally {
      setDqeAiLoading(false);
    }
  }

  /**
   * Importe les lignes "propres" renvoyees par l'analyse IA.
   *
   * Ici, on ne reparse pas le document une seconde fois.
   * On reutilise directement les donnees enrichies et nettoyees
   * pour creer de vraies lignes DQE dans le projet actif.
   */
  async function handleImportAiCleanData() {
    if (!dqeAiResult?.lignes?.length) {
      setDqeAiError("Aucune ligne AI n'est disponible a importer.");
      return;
    }

    try {
      const targetProjectId = await ensureTargetProjectIdForDqeImport("Projet DQE AI");
      setDqeAiImportLoading(true);
      setDqeAiError("");
      setDqeAiImportMessage("");

      const importableLines = dqeAiResult.lignes.filter(
        (line) =>
          line.designation &&
          line.lot &&
          line.famille &&
          line.prixLocalEstime != null &&
          (line.quantite ?? 0) > 0
      );

      if (importableLines.length === 0) {
        throw new Error("Aucune ligne suffisamment propre n'a ete detectee pour l'import.");
      }

      for (const line of importableLines) {
        const response = await apiFetch(`/dqe/items`, {
          method: "POST",
          headers: buildAuthHeaders(),
          body: JSON.stringify({
            projectId: Number(targetProjectId),
            lot: line.lot,
            famille: line.famille,
            designation: line.designation,
            unite: line.unite || "U",
            quantite: line.quantite || 1,
            prixUnitaire: line.prixLocalEstime,
          }),
        });

        const payload = await response.json();

        if (!response.ok) {
          throw new Error(payload.message || "Impossible d'importer une ligne AI.");
        }
      }

      const skippedLines = dqeAiResult.lignes.length - importableLines.length;
      const skippedMessage =
        skippedLines > 0 ? ` ${skippedLines} ligne(s) ont ete ignorees car trop incertaines.` : "";

      setDqeAiImportMessage(
        `${importableLines.length} ligne(s) propres importee(s) dans le projet actif.${skippedMessage}`
      );

      await refreshAllData();
    } catch (aiImportError) {
      setDqeAiError(aiImportError.message);
    } finally {
      setDqeAiImportLoading(false);
    }
  }

  async function handleDqeFullAnalysis() {
    if (!dqeFullFile) {
      setDqeFullError("Choisis un document ou colle un tableau structure a analyser.");
      return;
    }

    await analyzeDqeFullFile(
      dqeFullFile,
      "Impossible d'executer l'analyse intelligente du DQE."
    );
  }

  /**
   * Charge le vrai PDF de demonstration embarque dans le frontend.
   *
   * Cette fonction est utile pour la page /demo :
   * l'utilisateur peut tester la chaine complete sans chercher un document.
   */
  async function handleRealDemoFullAnalysis() {
    try {
      setDqeFullLoading(true);
      setDqeFullError("");
      setDqeFullResult(null);
      setDqeFullImportMessage("");
      setDqeFullProgress(10);

      const demoFileResponse = await fetch(DEMO_DQE_FILE_URL);
      if (!demoFileResponse.ok) {
        throw new Error("Impossible de charger le PDF reel de demonstration.");
      }

      setDqeFullProgress(25);
      const demoBlob = await demoFileResponse.blob();
      const demoFile = new File([demoBlob], "DQE_MEDICAL_CENTER_13-08-2025.pdf", {
        type: "application/pdf",
      });

      setDqeFullFile(demoFile);
      setDqeStructuredText("");
      await analyzeDqeFullFile(demoFile, "Impossible d'analyser l'exemple reel.");
    } catch (fullError) {
      setDqeFullError(fullError.message);
      setDqeFullProgress(0);
    } finally {
      setDqeFullLoading(false);
    }
  }

  /**
   * Permet d'analyser un tableau DQE colle directement dans l'interface.
   *
   * On genere un petit fichier CSV en memoire pour reutiliser exactement
   * le meme pipeline backend que les uploads classiques.
   */
  async function handleStructuredDqeTextAnalysis() {
    if (!dqeStructuredText.trim()) {
      setDqeFullError("Colle d'abord un tableau structure avant de lancer l'analyse.");
      return;
    }

    const structuredFile = new File([dqeStructuredText], "dqe-structure.csv", {
      type: "text/csv;charset=utf-8",
    });

    setDqeFullFile(structuredFile);
    await analyzeDqeFullFile(
      structuredFile,
      "Impossible d'analyser le tableau structure colle."
    );
  }

  async function handleStructuredDqeTextDirectImport() {
    if (!dqeStructuredText.trim()) {
      setDqeFullError("Colle d'abord un tableau structure avant l'import direct.");
      return;
    }

    try {
      const targetProjectId = await ensureTargetProjectIdForDqeImport("Projet DQE colle");
      setDqeFullImportLoading(true);
      setDqeFullError("");
      setDqeFullImportMessage("");

      const parsedLines = parseStructuredDqeTextToImportLines(dqeStructuredText);

      if (parsedLines.length === 0) {
        throw new Error("Aucune ligne structurée exploitable n'a été trouvée dans le tableau collé.");
      }

      const response = await apiFetch(`/dqe/import`, {
        method: "POST",
        headers: buildAuthHeaders(),
        body: JSON.stringify({
          projectId: Number(targetProjectId),
          lignes: parsedLines,
          replaceExisting: true,
        }),
      });

      const payload = await response.json();

      if (!response.ok) {
        throw new Error(payload.message || "Impossible d'importer directement le tableau colle.");
      }

      setDqeFullImportMessage(
        `${payload.lignesImportees} ligne(s) du tableau colle importee(s) avec succes. Les dashboards ont ete recalibres sur ce nouveau jeu de donnees.`
      );
      clearFilters();
      await refreshAllData();
    } catch (structuredImportError) {
      setDqeFullError(structuredImportError.message);
    } finally {
      setDqeFullImportLoading(false);
    }
  }

  async function handleImportValidDqeLines() {
    if (!dqeFullEditableLines?.length) {
      setDqeFullError("Aucune ligne analysee n'est disponible.");
      return;
    }

    try {
      const targetProjectId = await ensureTargetProjectIdForDqeImport("Projet DQE valide");
      setDqeFullImportLoading(true);
      setDqeFullError("");
      setDqeFullImportMessage("");

      const validLines = dqeFullEditableLines.filter(isImportableDqeLine);

      if (validLines.length === 0) {
        throw new Error("Aucune ligne corrigée et importable n'est disponible pour l'import.");
      }

      const response = await apiFetch(`/dqe/import`, {
        method: "POST",
        headers: buildAuthHeaders(),
        body: JSON.stringify({
          projectId: Number(targetProjectId),
          lignes: validLines,
          replaceExisting: true,
        }),
      });

      const payload = await response.json();

      if (!response.ok) {
        throw new Error(payload.message || "Impossible d'importer les lignes valides.");
      }

      setDqeFullImportMessage(
        `${payload.lignesImportees} ligne(s) valide(s) importee(s) avec succes. Les anciennes lignes du projet ont ete remplacees pour recalibrer les dashboards.`
      );
      clearFilters();
      await refreshAllData();
    } catch (importError) {
      setDqeFullError(importError.message);
    } finally {
      setDqeFullImportLoading(false);
    }
  }

  function updateDqeFullLine(index, fieldName, value) {
    setDqeFullEditableLines((currentLines) =>
      currentLines.map((line, lineIndex) => {
        if (lineIndex !== index) {
          return line;
        }

        const nextValue = isEditableNumericField(fieldName) ? normalizeNumericValue(value) : value;
        const nextLine = {
          ...line,
          [fieldName]: nextValue,
        };

        return {
          ...nextLine,
          erreurs: recomputeEditableLineErrors(nextLine),
          valide: isImportableDqeLine(nextLine),
        };
      })
    );
  }

  function resetDqeFullCorrections() {
    setDqeFullEditableLines(dqeFullResult?.lignes ? dqeFullResult.lignes.map((line) => ({ ...line })) : []);
  }

  async function analyzeDqeFullFile(file, fallbackMessage) {
    try {
      setDqeFullLoading(true);
      setDqeFullError("");
      setDqeFullResult(null);
      setDqeFullImportMessage("");
      setDqeFullProgress(15);

      const formData = new FormData();
      formData.append("file", file);

      const response = await apiFetch(`/dqe/analyze-full`, {
        method: "POST",
        headers: buildAuthHeaders(false),
        body: formData,
      });

      setDqeFullProgress(70);
      const payload = await response.json();

      if (!response.ok) {
        throw new Error(payload.message || fallbackMessage);
      }

      setDqeFullResult(payload);
      setDqeFullProgress(100);
    } catch (fullError) {
      setDqeFullError(fullError.message);
      setDqeFullProgress(0);
    } finally {
      setDqeFullLoading(false);
    }
  }

  return {
    filters: {
      lotFilter,
      familleFilter,
      batimentFilter,
      niveauFilter,
      setLotFilter,
      setFamilleFilter,
      setBatimentFilter,
      setNiveauFilter,
      clearFilters,
    },
    projectContext: {
      activeProjectId,
      setActiveProjectId,
      selectedProjectId,
      setSelectedProjectId,
      projects,
      activeProject,
      activeCurrencyCode,
      projectCreationName,
      setProjectCreationName,
      projectCreationLoading,
      projectCreationError,
      projectForm,
      updateField: updateProjectFormField,
      updateBuilding: updateProjectBuilding,
      addBuilding: addProjectBuilding,
      removeBuilding: removeProjectBuilding,
    },
    auth: {
      authToken,
      authEmail,
      authUserId,
      authLoading,
      authError,
      isAuthenticated: Boolean(authToken),
      login,
      register,
      logout,
      createProject,
    },
    data: {
      summary,
      items,
      allItems,
      recentItems,
      planningTasks,
      projects,
      loading,
      catalogLoading,
      planningLoading,
      error,
      planningError,
      backendWakeInProgress,
      backendStatusMessage,
      chartData: buildChartData(summary, lotFilter),
      familyEntries: buildFamilyEntries(summary, familleFilter),
      batimentEntries: buildGroupedEntries(items, "batiment", batimentFilter),
      niveauEntries: buildGroupedEntries(items, "niveau", niveauFilter),
      batimentNiveauHeatmap: buildBatimentNiveauHeatmap(items, batimentFilter, niveauFilter),
      zoneEntries: buildZoneEntries(items),
      coverageMetrics: buildCoverageMetrics(items),
      topEconomies: buildTopEconomies(items),
      lotOptions: extractOptions(allItems, "lot").map((value) => ({
        value,
        label: normalizeLotLabel(value),
      })),
      familleOptions: extractOptions(allItems, "famille").map((value) => ({
        value,
        label: normalizeFamilyLabel(value),
      })),
      batimentOptions: extractOptions(allItems, "batiment").map((value) => ({
        value,
        label: normalizeZoneLabel(value),
      })),
      niveauOptions: extractOptions(allItems, "niveau").map((value) => ({
        value,
        label: normalizeZoneLabel(value),
      })),
    },
    importState: {
      selectedProjectId,
      selectedFile,
      importLoading,
      importMessage,
      importError,
      setSelectedProjectId,
      setSelectedFile,
      handleImport,
      handleTemplateDownload,
    },
    dqeState: {
      selectedProjectId,
      draft: dqeDraft,
      draftLines: dqeDraftLines,
      prixTotal: dqePrixTotal,
      builderLoading: dqeBuilderLoading,
      builderMessage: dqeBuilderMessage,
      builderError: dqeBuilderError,
      familySuggestions: dqeFamilySuggestions,
      documentFile: dqeDocumentFile,
      documentLoading: dqeDocumentLoading,
      documentMessage: dqeDocumentMessage,
      documentError: dqeDocumentError,
      analysisFile: dqeAnalysisFile,
      analysisLoading: dqeAnalysisLoading,
      analysisError: dqeAnalysisError,
      analysisResult: dqeAnalysisResult,
      analysisImportLoading: dqeAnalysisImportLoading,
      analysisImportMessage: dqeAnalysisImportMessage,
      setSelectedProjectId,
      updateDraft: updateDqeDraft,
      addDraftLine: addDqeDraftLine,
      removeDraftLine: removeDqeDraftLine,
      saveDraftLines: saveDqeDraftLines,
      handleDqeExport,
      setDqeDocumentFile,
      handleDqeDocumentImport,
      setDqeAnalysisFile,
      handleDqeAnalysis,
      handleRealDemoAnalysis,
      handleImportAnalyzedFile,
    },
    dqeAiState: {
      file: dqeAiFile,
      loading: dqeAiLoading,
      error: dqeAiError,
      result: dqeAiResult,
      importLoading: dqeAiImportLoading,
      importMessage: dqeAiImportMessage,
      setFile: setDqeAiFile,
      handleAnalyze: handleDqeAiAnalysis,
      handleImportCleanData: handleImportAiCleanData,
    },
    dqeFullState: {
      file: dqeFullFile,
      structuredText: dqeStructuredText,
      loading: dqeFullLoading,
      error: dqeFullError,
      result: dqeFullResult,
      editableLines: dqeFullEditableLines,
      importLoading: dqeFullImportLoading,
      importMessage: dqeFullImportMessage,
      progress: dqeFullProgress,
      setFile: setDqeFullFile,
      setStructuredText: setDqeStructuredText,
      updateLine: updateDqeFullLine,
      resetCorrections: resetDqeFullCorrections,
      handleAnalyze: handleDqeFullAnalysis,
      handleAnalyzeStructuredText: handleStructuredDqeTextAnalysis,
      handleDirectImportStructuredText: handleStructuredDqeTextDirectImport,
      handleRealDemoAnalysis: handleRealDemoFullAnalysis,
      handleImportValidLines: handleImportValidDqeLines,
    },
    actions: {
      refreshAllData,
    },
  };

  async function login(credentials) {
    await authenticate("/auth/login", credentials);
  }

  async function register(credentials) {
    await authenticate("/auth/register", credentials);
  }

  async function apiFetch(path, options = {}, attempt = 0) {
    const requestUrl = path.startsWith("http://") || path.startsWith("https://")
      ? path
      : `${API_URL}${path}`;

    try {
      const response = await fetch(requestUrl, options);

      if (RENDER_RETRYABLE_STATUSES.has(response.status) && attempt < RENDER_COLD_START_RETRIES) {
        setBackendWakeInProgress(true);
        setBackendStatusMessage("Backend en cours de demarrage sur Render, nouvelle tentative...");
        await wait(RENDER_COLD_START_DELAY_MS);
        return apiFetch(path, options, attempt + 1);
      }

      if (response.ok && (backendWakeInProgress || backendStatusMessage)) {
        setBackendWakeInProgress(false);
        setBackendStatusMessage("");
      }

      return response;
    } catch (requestError) {
      if (attempt < RENDER_COLD_START_RETRIES) {
        setBackendWakeInProgress(true);
        setBackendStatusMessage("Backend en cours de reveil, nouvelle tentative...");
        await wait(RENDER_COLD_START_DELAY_MS);
        return apiFetch(path, options, attempt + 1);
      }

      setBackendWakeInProgress(false);
      throw requestError;
    }
  }

  async function authenticate(path, credentials) {
    try {
      setAuthLoading(true);
      setAuthError("");

      const response = await apiFetch(path, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(credentials),
      });

      const payload = await response.json();

      if (!response.ok) {
        throw new Error(payload.message || "Authentification impossible.");
      }

      persistAuthSession(payload);
    } catch (authenticationError) {
      setAuthError(authenticationError.message);
    } finally {
      setAuthLoading(false);
    }
  }

  function logout() {
    window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
    window.localStorage.removeItem(AUTH_EMAIL_STORAGE_KEY);
    window.localStorage.removeItem(AUTH_USER_ID_STORAGE_KEY);
    setAuthToken("");
    setAuthEmail("");
    setAuthUserId("");
  }

  async function createProject(payloadOverride) {
    const projectPayload = payloadOverride ?? { name: projectCreationName };

    if (!projectPayload.name?.trim()) {
      setProjectCreationError("Le nom du projet est obligatoire.");
      return null;
    }

    try {
      setProjectCreationLoading(true);
      setProjectCreationError("");

      const response = await apiFetch(`/projects`, {
        method: "POST",
        headers: buildAuthHeaders(),
        body: JSON.stringify(projectPayload),
      });

      const createdProject = await response.json();

      if (!response.ok) {
        throw new Error(createdProject.message || "Impossible de creer le projet.");
      }

      setProjectCreationName("");
      setProjectForm(createDefaultProjectForm());
      await loadProjects();
      const newProjectId = String(createdProject.id);
      setActiveProjectId(newProjectId);
      setSelectedProjectId(newProjectId);
      return createdProject;
    } catch (creationError) {
      setProjectCreationError(creationError.message);
      return null;
    } finally {
      setProjectCreationLoading(false);
    }
  }

  function persistAuthSession(payload) {
    const nextToken = payload.token ?? "";
    const nextEmail = payload.email ?? "";
    const nextUserId = payload.userId != null ? String(payload.userId) : "";

    window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, nextToken);
    window.localStorage.setItem(AUTH_EMAIL_STORAGE_KEY, nextEmail);
    window.localStorage.setItem(AUTH_USER_ID_STORAGE_KEY, nextUserId);

    setAuthToken(nextToken);
    setAuthEmail(nextEmail);
    setAuthUserId(nextUserId);
  }

  function updateProjectFormField(fieldName, value) {
    setProjectForm((currentForm) => ({
      ...currentForm,
      [fieldName]: value,
    }));
    setProjectCreationError("");
  }

  function updateProjectBuilding(index, fieldName, value) {
    setProjectForm((currentForm) => ({
      ...currentForm,
      batiments: currentForm.batiments.map((building, buildingIndex) =>
        buildingIndex === index
          ? {
              ...building,
              [fieldName]: value,
            }
          : building
      ),
    }));
  }

  function addProjectBuilding() {
    setProjectForm((currentForm) => ({
      ...currentForm,
      batiments: [
        ...currentForm.batiments,
        {
          nom: "",
          etagesText: "",
        },
      ],
    }));
  }

  function removeProjectBuilding(indexToRemove) {
    setProjectForm((currentForm) => ({
      ...currentForm,
      batiments: currentForm.batiments.filter((_, buildingIndex) => buildingIndex !== indexToRemove),
    }));
  }
}

function buildAuthHeaders(includeJsonContentType = true) {
  const headers = {};
  const token = window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);

  if (includeJsonContentType) {
    headers["Content-Type"] = "application/json";
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

function validateDqeDraftLine(draft) {
  if (!draft.lot.trim()) {
    return "Le lot est obligatoire.";
  }
  if (!draft.designation.trim()) {
    return "La designation est obligatoire.";
  }
  if (normalizeNumericValue(draft.quantite) <= 0) {
    return "La quantite doit etre > 0.";
  }
  if (normalizeNumericValue(draft.prixUnitaire) <= 0) {
    return "Le prix unitaire doit etre > 0.";
  }
  return "";
}

function normalizeNumericValue(value) {
  if (value == null || value === "") {
    return 0;
  }
  return Number(String(value).replace(",", ".")) || 0;
}

function wait(durationMs) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, durationMs);
  });
}

function isEditableNumericField(fieldName) {
  return [
    "quantite",
    "prixUnitaire",
    "prixLocalEstime",
    "prixImportEstime",
    "prixImportRendu",
    "scoreConfiance",
  ].includes(fieldName);
}

function recomputeEditableLineErrors(line) {
  const remainingErrors = (line.erreurs || []).filter(
    (error) =>
      ![
        "PRIX_MANQUANT",
        "QUANTITE_MANQUANTE",
        "NON_CLASSE",
        "BATIMENT_NON_IDENTIFIE",
        "NIVEAU_NON_IDENTIFIE",
      ].includes(error)
  );

  if (!line.quantite || line.quantite <= 0) {
    remainingErrors.push("QUANTITE_MANQUANTE");
  }
  if ((line.prixLocalEstime ?? line.prixUnitaire ?? 0) <= 0) {
    remainingErrors.push("PRIX_MANQUANT");
  }
  if (!line.lot || !line.famille) {
    remainingErrors.push("NON_CLASSE");
  }
  if (!line.batiment || String(line.batiment).includes("A_VERIFIER")) {
    remainingErrors.push("BATIMENT_NON_IDENTIFIE");
  }
  if (!line.niveau || String(line.niveau).includes("A_VERIFIER")) {
    remainingErrors.push("NIVEAU_NON_IDENTIFIE");
  }

  return [...new Set(remainingErrors)];
}

function isImportableDqeLine(line) {
  return Boolean(
    line.designation
      && line.lot
      && line.famille
      && line.batiment
      && !String(line.batiment).includes("A_VERIFIER")
      && line.niveau
      && !String(line.niveau).includes("A_VERIFIER")
      && (line.quantite ?? 0) > 0
      && ((line.prixLocalEstime ?? line.prixUnitaire ?? 0) > 0)
  );
}

function triggerBrowserDownload(blob, fileName) {
  const downloadUrl = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = downloadUrl;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(downloadUrl);
}

function parseStructuredDqeTextToImportLines(text) {
  const lines = text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);

  if (lines.length <= 1) {
    return [];
  }

  const delimiter = lines[0].includes("\t") ? "\t" : ";";
  const headers = lines[0].split(delimiter).map((header) => normalizeStructuredHeader(header));
  const headerIndex = new Map(headers.map((header, index) => [header, index]));

  const requiredHeaders = ["lot", "batiment", "niveau", "designation", "unite", "quantite", "pu"];
  const hasRequiredHeaders = requiredHeaders.every((header) => headerIndex.has(header));

  if (!hasRequiredHeaders) {
    throw new Error("Le tableau colle ne contient pas toutes les colonnes attendues : Lot, Batiment, Niveau, Designation, Unite, Quantite, PU.");
  }

  return lines
    .slice(1)
    .map((line) => line.split(delimiter))
    .filter((columns) => columns.some((column) => column?.trim()))
    .map((columns) => {
      const lotCode = readStructuredColumn(columns, headerIndex, "lot");
      const sousLot = readStructuredColumn(columns, headerIndex, "souslot");
      const designation = readStructuredColumn(columns, headerIndex, "designation");
      const unite = readStructuredColumn(columns, headerIndex, "unite");
      const batiment = readStructuredColumn(columns, headerIndex, "batiment");
      const niveau = readStructuredColumn(columns, headerIndex, "niveau");
      const quantite = normalizeNumericValue(readStructuredColumn(columns, headerIndex, "quantite"));
      const prixUnitaire = normalizeNumericValue(readStructuredColumn(columns, headerIndex, "pu"));
      const total = normalizeNumericValue(readStructuredColumn(columns, headerIndex, "total"));

      return {
        designation,
        quantite,
        unite,
        prixUnitaire,
        total,
        lot: resolveStructuredLotLabel(lotCode),
        famille: sousLot || resolveStructuredLotLabel(lotCode),
        batiment,
        niveau,
        erreurs: [],
        valide: true,
        scoreQualite: 100,
        prixLocalEstime: prixUnitaire,
        prixImportEstime: null,
        prixImportRendu: null,
        decision: "LOCAL",
        risque: "FAIBLE",
        fournisseurSuggestion: "",
        scoreConfiance: 100,
      };
    })
    .filter((line) => line.designation && line.quantite > 0 && line.prixUnitaire > 0);
}

function normalizeStructuredHeader(header) {
  return String(header || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]/g, "");
}

function readStructuredColumn(columns, headerIndex, headerName) {
  const index = headerIndex.get(headerName);
  if (index == null) {
    return "";
  }

  return String(columns[index] ?? "").trim();
}

function resolveStructuredLotLabel(lotCode) {
  const normalizedCode = String(lotCode || "").trim();
  return LOT_LABELS_BY_CODE[normalizedCode] || normalizedCode || "DQE";
}

