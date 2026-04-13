// ===============================
// 1. IMPORTS
// ===============================
// On importe React pour gerer les champs du formulaire,
// puis React Router pour naviguer entre les pages sans recharger l'application.
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { formatCurrency, formatNumber } from "../lib/capex";

// ===============================
// 2. RACCOURCIS WORKSPACE
// ===============================
// Cette liste alimente le mode "dashboard" de la home quand l'utilisateur est connecte.
// Le but est de transformer la page d'accueil en hub de travail.
const DASHBOARD_SHORTCUTS = [
  {
    title: "Projet",
    description: "Structurer le projet, ses hypotheses et son cadre CAPEX.",
    to: "/project",
    action: "Ouvrir",
  },
  {
    title: "DQE intelligent",
    description: "Charger, enrichir et corriger un DQE avant import.",
    to: "/demo",
    action: "Ouvrir",
  },
  {
    title: "Direction",
    description: "Lire les KPI, la synthese CAPEX et les arbitrages.",
    to: "/direction",
    action: "Piloter",
  },
  {
    title: "Chantier",
    description: "Suivre le planning, les couts et l'avancement terrain.",
    to: "/chantier",
    action: "Suivre",
  },
];

// ===============================
// 3. COMPOSANT PRINCIPAL
// ===============================
// Cette page sert a la fois de landing page marketing
// et de point d'entree rapide vers les workflows metier.
export default function HomePremium({ dashboard }) {
  const navigate = useNavigate();

  // On recupere le module d'authentification partage par toute l'application.
  const auth = dashboard?.auth;

  // On recupere le contexte projet pour savoir s'il existe deja un projet actif.
  const projectContext = dashboard?.projectContext;

  // useState permet de memoriser les valeurs tapees dans les champs du formulaire.
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const previousAuthRef = useRef(auth?.isAuthenticated ?? false);

  // On recherche le projet actif pour personnaliser la home en mode connecte.
  const activeProject =
    projectContext?.projects?.find((project) => String(project.id) === projectContext.activeProjectId) ?? null;
  const summary = dashboard?.data?.summary;
  const coverage = dashboard?.data?.coverageMetrics;
  const currencyCode = activeProject?.currencyCode || "XAF";

  // Cette fonction centralise le bon point d'entree apres connexion.
  // Si un projet existe deja, on va sur la vue projet.
  // Sinon on envoie vers la creation de projet.
  function openPostLoginDestination() {
    if (projectContext?.projects?.length) {
      navigate("/project");
      return;
    }

    navigate("/projects/create");
  }

  // Quand l'utilisateur vient juste de se connecter,
  // on le redirige automatiquement vers son dernier projet
  // ou vers la creation de projet si aucun n'existe encore.
  useEffect(() => {
    const wasAuthenticated = previousAuthRef.current;
    const isAuthenticated = auth?.isAuthenticated ?? false;

    if (!wasAuthenticated && isAuthenticated) {
      openPostLoginDestination();
    }

    previousAuthRef.current = isAuthenticated;
  }, [auth?.isAuthenticated, projectContext?.projects?.length]);

  // Cette fonction appelle le backend pour connecter l'utilisateur.
  async function handleLogin() {
    if (!auth) {
      return;
    }

    await auth.login({ email, password });
  }

  // Cette fonction appelle le backend pour creer un compte.
  async function handleRegister() {
    if (!auth) {
      return;
    }

    await auth.register({ email, password });
  }

  // Cette fonction envoie l'utilisateur connecte vers le bon point d'entree.
  function handleOpenWorkspace() {
    openPostLoginDestination();
  }

  // ===============================
  // 4. AFFICHAGE
  // ===============================
  return (
    <div className="landing sp2i-home">
      <section className="sp2i-home-shell">
        <header className="sp2i-home-header">
          <p className="landing-kicker">SP2I BUILD</p>
          <h1>SP2I BUILD - BI Workspace</h1>
          <p className="subtitle">Pilotage CAPEX - Construction &amp; Immobilier</p>
          <p className="sp2i-home-signature">By Paterne Vladimir Gankama</p>
        </header>

        {auth?.isAuthenticated ? (
          <section className="sp2i-home-workspace">
            <article className="sp2i-home-card sp2i-workspace-hero">
              <div className="sp2i-card-head">
                <p className="panel-label">Workspace connecte</p>
                <h2>Bienvenue dans votre cockpit SP2I BUILD</h2>
              </div>

              <p className="sp2i-demo-description">
                Vous etes connecte avec {auth.authEmail}. Reprenez votre projet actif ou ouvrez
                directement un workflow metier.
              </p>

              <div className="sp2i-workspace-meta">
                <div className="sp2i-workspace-pill">
                  <span>Projet actif</span>
                  <strong>{activeProject?.name ?? "Aucun projet configure"}</strong>
                </div>

                <div className="sp2i-workspace-pill">
                  <span>Plateforme</span>
                  <strong>Workspace SaaS CAPEX</strong>
                </div>
              </div>

              <div className="sp2i-workspace-kpis">
                <div className="sp2i-workspace-kpi">
                  <span>CAPEX brut</span>
                  <strong>{formatCurrency(summary?.capexBrut ?? 0, currencyCode)}</strong>
                </div>
                <div className="sp2i-workspace-kpi">
                  <span>Lignes actives</span>
                  <strong>{formatNumber(coverage?.totalLines ?? 0)}</strong>
                </div>
                <div className="sp2i-workspace-kpi">
                  <span>Batiments</span>
                  <strong>{formatNumber(coverage?.buildings ?? 0)}</strong>
                </div>
                <div className="sp2i-workspace-kpi">
                  <span>Niveaux</span>
                  <strong>{formatNumber(coverage?.levels ?? 0)}</strong>
                </div>
              </div>

              <div className="sp2i-auth-actions">
                <button className="sp2i-login-button" type="button" onClick={handleOpenWorkspace}>
                  {activeProject ? "Ouvrir la vue projet" : "Creer un nouveau projet"}
                </button>

                <button className="sp2i-register-button" type="button" onClick={() => navigate("/direction")}>
                  Ouvrir les KPI
                </button>
              </div>
            </article>

            <section className="sp2i-workspace-grid">
              {DASHBOARD_SHORTCUTS.map((shortcut) => (
                <article className="sp2i-home-card sp2i-shortcut-card" key={shortcut.title}>
                  <p className="panel-label">Workflow</p>
                  <h3>{shortcut.title}</h3>
                  <p>{shortcut.description}</p>

                  <button className="ghost-button" type="button" onClick={() => navigate(shortcut.to)}>
                    {shortcut.action}
                  </button>
                </article>
              ))}
            </section>
          </section>
        ) : null}

        <section className="sp2i-home-grid">
          <article className="sp2i-home-card">
            <div className="sp2i-card-head">
              <p className="panel-label">Acces securise</p>
              <h2>Authentification</h2>
            </div>

            <label className="sp2i-auth-field">
              <span>Email</span>
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="vous@entreprise.com"
                autoComplete="email"
              />
            </label>

            <label className="sp2i-auth-field">
              <span>Mot de passe</span>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Votre mot de passe"
                autoComplete="current-password"
              />
            </label>

            <div className="sp2i-auth-actions">
              <button
                className="sp2i-login-button"
                type="button"
                disabled={auth?.authLoading}
                onClick={handleLogin}
              >
                {auth?.authLoading ? "Connexion..." : "Login"}
              </button>

              <button
                className="sp2i-register-button"
                type="button"
                disabled={auth?.authLoading}
                onClick={handleRegister}
              >
                Register
              </button>
            </div>

            {auth?.authError ? <p className="error-text">{auth.authError}</p> : null}

            {auth?.isAuthenticated ? (
              <div className="sp2i-auth-success">
                <p>Connecte en tant que {auth.authEmail}.</p>

                <button className="ghost-button" type="button" onClick={handleOpenWorkspace}>
                  Ouvrir mon espace projet
                </button>
              </div>
            ) : null}
          </article>

          <article className="sp2i-home-card">
            <div className="sp2i-card-head">
              <p className="panel-label">Exploration sans compte</p>
              <h2>Projet demo</h2>
            </div>

            <p className="sp2i-demo-description">
              Decouvrez toutes les fonctionnalites sans creer de compte :
            </p>

            <ul className="sp2i-demo-list">
              <li>Dashboard CAPEX complet</li>
              <li>DQE intelligent avec scoring IA</li>
              <li>Audit Import Chine</li>
              <li>Planning &amp; ordonnancement</li>
              <li>Heatmap batiments / niveaux</li>
              <li>Comparaison DQE</li>
            </ul>

            <button className="sp2i-demo-button" type="button" onClick={() => navigate("/demo")}>
              Lancer le projet demo
            </button>
          </article>
        </section>

        <section className="sp2i-home-banner">
          <p>Vous etes deja client ? Connectez-vous pour acceder a vos projets.</p>
          <p>Nouveau ? Creez un compte ou testez le projet demo.</p>
        </section>

        <footer className="sp2i-home-footer">
          <strong>SP2I BUILD - BI Workspace</strong>
          <span>By Paterne Vladimir Gankama</span>
          <span>Pilotage CAPEX - Construction &amp; Immobilier</span>
        </footer>
      </section>
    </div>
  );
}
