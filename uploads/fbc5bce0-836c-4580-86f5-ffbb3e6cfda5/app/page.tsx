"use client"

import type React from "react"

import { useState, useMemo } from "react"
import { Github, Linkedin, Phone, X, Target, Cpu, ListChecks } from "lucide-react"

// Projects Data
const projects = [
  {
    title: "Al Baraka Digital V1 - Plateforme Bancaire Sécurisée",
    description:
      "Plateforme bancaire sécurisée avec JWT pour digitaliser les opérations bancaires : dépôts, retraits, virements avec validation automatique selon montant.",
    technologies: ["Spring Boot", "JWT", "OAuth2", "MySQL", "Docker"],
    logos: [
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mysql/mysql-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/docker/docker-original.svg",
    ],
    cahier: [
      "Authentification JWT stateless + OAuth2 pour agents bancaires",
      "Gestion des opérations : dépôts, retraits, virements",
      "Validation automatique pour montants ≤ 10 000 DH",
      "Upload justificatifs pour opérations > 10 000 DH",
      "Workflow de validation manuelle par agent",
      "Conteneurisation Docker complète",
    ],
  },
  {
    title: "Al Baraka Digital V2 - Banking Platform + AI",
    description:
      "Évolution avec validation intelligente par Spring AI, analyse automatique des justificatifs, CI/CD et interface Thymeleaf sécurisée.",
    technologies: ["Spring Boot", "Spring AI", "Thymeleaf", "PostgreSQL", "Docker", "GitHub Actions"],
    logos: [
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/postgresql/postgresql-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/docker/docker-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/github/github-original.svg",
    ],
    cahier: [
      "Spring AI pour analyse intelligente des justificatifs",
      "Recommandations automatiques : APPROVE, REJECT, NEED_HUMAN_REVIEW",
      "Interface web sécurisée avec Thymeleaf + Remember-me",
      "Pipeline CI/CD avec GitHub Actions",
      "Traçabilité complète des opérations",
      "Architecture prête pour production",
    ],
  },
  {
    title: "SmartShop - API Gestion Commerciale B2B",
    description:
      "API REST backend pour distributeur B2B avec 650+ clients, système de fidélité, commandes multi-produits et paiements fractionnés.",
    technologies: ["Spring Boot", "PostgreSQL", "MapStruct", "Lombok", "Swagger"],
    logos: [
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/postgresql/postgresql-original.svg",
    ],
    cahier: [
      "Gestion clients avec niveaux de fidélité : BASIC → SILVER → GOLD → PLATINUM",
      "Commandes multi-produits avec vérification stock temps réel",
      "Remises automatiques selon fidélité et codes promo",
      "Calculs automatiques HT, TVA 20%, TTC",
      "Paiements fractionnés avec limite légale 20 000 DH",
      "Documentation Swagger complète",
    ],
  },
  {
    title: "Plateforme E-Learning",
    description:
      "Plateforme complète type Udemy permettant aux étudiants de suivre des cours en ligne et aux enseignants de publier du contenu éducatif.",
    technologies: ["PHP", "MySQL", "JavaScript", "Tailwind CSS"],
    logos: [
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/php/php-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mysql/mysql-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/javascript/javascript-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/tailwindcss/tailwindcss-original.svg",
    ],
    cahier: [
      "Authentification sécurisée pour étudiants et enseignants",
      "Système de gestion de cours avec vidéos et ressources",
      "Suivi de progression et quiz interactifs",
      "Interface d'administration complète",
    ],
  },
  {
    title: "Nostalgia - Enchères Culturelles",
    description:
      "Plateforme d'enchères en ligne pour objets culturels rares avec système sécurisé, blog interactif et paiement PayPal.",
    technologies: ["Laravel", "PostgreSQL", "Tailwind CSS", "JavaScript"],
    logos: [
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/laravel/laravel-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/postgresql/postgresql-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/tailwindcss/tailwindcss-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/javascript/javascript-original.svg",
    ],
    cahier: [
      "Système d'enchères en temps réel",
      "Intégration paiement PayPal sécurisé",
      "Blog interactif avec commentaires",
      "Gestion des objets culturels et historiques",
    ],
  },
  {
    title: "Tricol - Gestion de Commandes",
    description:
      "API REST pour la gestion complète des fournisseurs, produits, commandes avec valorisation automatique et documentation Swagger.",
    technologies: ["Spring Boot", "Angular", "PostgreSQL"],
    logos: [
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/angularjs/angularjs-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/postgresql/postgresql-original.svg",
    ],
    cahier: [
      "API REST avec documentation Swagger",
      "Gestion fournisseurs et produits",
      "Système de commandes avec valorisation",
      "Architecture microservices",
    ],
  },
  {
    title: "SEBUL Back Office",
    description:
      "Système interne pour gérer équipements, logistique, maintenance et équipes avec interfaces administratives avancées.",
    technologies: ["Symfony", "MySQL", "Bootstrap", "JavaScript"],
    logos: [
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/symfony/symfony-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mysql/mysql-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/bootstrap/bootstrap-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/javascript/javascript-original.svg",
    ],
    cahier: [
      "Gestion d'équipements et logistique",
      "Système de maintenance préventive",
      "Gestion des équipes et planning",
      "Tableaux de bord administratifs",
    ],
  },
  {
    title: "E-Commerce Dashboard",
    description:
      "Tableau de bord complet pour la gestion d'une boutique en ligne avec statistiques en temps réel et gestion des commandes.",
    technologies: ["React", "Laravel", "MySQL", "Tailwind CSS"],
    logos: [
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/react/react-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/laravel/laravel-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mysql/mysql-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/tailwindcss/tailwindcss-original.svg",
    ],
    cahier: [
      "Statistiques en temps réel",
      "Gestion des produits et inventaire",
      "Système de commandes et paiements",
      "Analyses et rapports détaillés",
    ],
  },
  {
    title: "Gestion RH & Paie",
    description:
      "Application de gestion des ressources humaines avec système de paie, congés, absences et évaluation des performances.",
    technologies: ["Spring Boot", "Angular", "PostgreSQL"],
    logos: [
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/angularjs/angularjs-original.svg",
      "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/postgresql/postgresql-original.svg",
    ],
    cahier: [
      "Système de paie automatisé",
      "Gestion des congés et absences",
      "Évaluation des performances",
      "Rapports RH détaillés",
    ],
  },
]

// Star component for background animation
function Star({ size, top, left, duration }: { size: number; top: string; left: string; duration: number }) {
  return (
    <div
      className="star"
      style={
        {
          width: `${size}px`,
          height: `${size}px`,
          top,
          left,
          animationDuration: `${duration}s`,
        } as React.CSSProperties & { "--duration": string }
      }
    />
  )
}

export default function Portfolio() {
  // State for cursor animation
  const [isAnimating, setIsAnimating] = useState(false)

  // Stars generation
  const stars = useMemo(() => {
    return Array.from({ length: 100 }).map((_, i) => ({
      id: i,
      size: Math.random() * 3 + 1,
      top: `${Math.random() * 100}%`,
      left: `${Math.random() * 100}%`,
      duration: Math.random() * 3 + 2,
    }))
  }, [])

  return (
    <main className="bg-background text-foreground min-h-screen relative overflow-hidden">
      {/* Stars Background */}
      <div className="stars-background">
        {stars.map((star) => (
          <Star key={star.id} size={star.size} top={star.top} left={star.left} duration={star.duration} />
        ))}
      </div>

      {/* Navigation */}
      <nav className="fixed top-0 w-full bg-background/95 backdrop-blur-sm z-50 border-b border-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="text-2xl font-bold gradient-text">AH</div>
            <div className="hidden md:flex space-x-8">
              <a href="#accueil" className="hover:text-primary transition-colors">
                Accueil
              </a>
              <a href="#apropos" className="hover:text-primary transition-colors">
                À propos
              </a>
              <a href="#competences" className="hover:text-primary transition-colors">
                Compétences
              </a>
              <a href="#projets" className="hover:text-primary transition-colors">
                Projets
              </a>
              <a href="#contact" className="hover:text-primary transition-colors">
                Contact
              </a>
            </div>
          </div>
        </div>
      </nav>

      {/* Social Sidebar */}
      <div className="fixed right-6 top-1/2 -translate-y-1/2 z-40 hidden lg:flex flex-col items-center space-y-4">
        <div className="w-px h-20 bg-border"></div>
        <a
          href="https://github.com/AbdellatifHissoune"
          target="_blank"
          rel="noreferrer"
          className="text-secondary-foreground hover:text-primary transition-colors hover:-translate-y-1 transform duration-200"
        >
          <Github className="w-6 h-6" />
        </a>
        <a
          href="https://linkedin.com/in/abdellatif-hissoune"
          target="_blank"
          rel="noreferrer"
          className="text-secondary-foreground hover:text-primary transition-colors hover:-translate-y-1 transform duration-200"
        >
          <Linkedin className="w-6 h-6" />
        </a>
        <a
          href="tel:+212690732817"
          className="text-secondary-foreground hover:text-primary transition-colors hover:-translate-y-1 transform duration-200"
        >
          <Phone className="w-6 h-6" />
        </a>
        <div className="w-px h-20 bg-border"></div>
      </div>

      <div className="fixed right-32 top-1/2 -translate-y-1/2 z-30 hidden lg:block">
        <div className="hand-pointer">👉</div>
        <div className="rocket-launch">🚀</div>
      </div>

      {/* Hero Section */}
      <section id="accueil" className="min-h-screen flex items-center pt-24 pb-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid md:grid-cols-2 gap-12 items-center">
          <div className="fade-in-up">
            <h1 className="text-5xl md:text-7xl font-bold mb-6 leading-tight">
              Salut, je suis <span className="gradient-text">Abdellatif</span>
            </h1>
            <p className="text-xl md:text-2xl text-secondary-foreground mb-8">Développeur Full Stack Web</p>
            <p className="text-lg text-muted-foreground mb-12 max-w-xl">
              Passionné par la création d'applications web modernes et performantes avec les technologies les plus
              récentes.
            </p>
            <div className="flex flex-wrap gap-4">
              <a
                href="#projets"
                className="bg-primary hover:bg-primary/90 text-primary-foreground px-8 py-3 rounded-lg font-semibold transition"
              >
                Voir mes projets
              </a>
              <a
                href="#contact"
                className="border border-primary hover:bg-primary/10 px-8 py-3 rounded-lg font-semibold transition"
              >
                Me contacter
              </a>
            </div>
          </div>
          <div className="relative fade-in-up flex justify-center">
            <div className="relative w-64 h-64 md:w-80 md:h-80">
              <div className="absolute inset-0 bg-primary/20 blur-3xl rounded-full"></div>
              <img
                src="/abdellatif-portrait.jpg"
                alt="Abdellatif Hissoune"
                className="relative z-10 w-full h-full object-cover rounded-2xl border-2 border-border"
              />
            </div>
          </div>
        </div>
      </section>

      {/* About Section */}
      <section id="apropos" className="py-20 bg-card/30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-4xl font-bold mb-12 text-center gradient-text">À propos de moi</h2>
          <div className="grid md:grid-cols-2 gap-12 items-center">
            <div>
              <p className="text-foreground text-lg mb-6">
                Je suis un développeur Full Stack passionné, actuellement étudiant à YouCode (UM6P) à Safi. J'ai une
                forte expérience dans le développement d'applications web complètes, de la conception à la mise en
                production.
              </p>
              <p className="text-foreground text-lg mb-6">
                Mon parcours comprend un stage chez Proxisoft SARL où j'ai travaillé sur des projets Back Office
                complexes utilisant Symfony, ainsi que le développement de plus de 40 projets personnels couvrant
                diverses technologies.
              </p>
            </div>
            <div className="bg-card p-8 rounded-lg border border-border">
              <h3 className="text-2xl font-bold mb-6">Informations</h3>
              <div className="space-y-4">
                <div>
                  <p className="text-muted-foreground">Localisation</p>
                  <p className="text-foreground">Safi, Maroc</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Email</p>
                  <p className="text-foreground">haissouneabdellatif749@gmail.com</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Téléphone</p>
                  <p className="text-foreground">+212 690732817</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Formation</p>
                  <p className="text-foreground">YouCode (UM6P), Safi</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Skills Section */}
      <section id="competences" className="py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-4xl font-bold mb-12 text-center gradient-text">Compétences Techniques</h2>

          <div className="grid md:grid-cols-3 gap-8">
            {/* Front-End */}
            <div className="bg-card p-8 rounded-lg border border-border">
              <h3 className="text-2xl font-bold mb-6 text-primary">Front-End</h3>
              <div className="grid grid-cols-3 gap-6">
                {[
                  { name: "HTML", icon: "html5" },
                  { name: "CSS", icon: "css3" },
                  { name: "JavaScript", icon: "javascript" },
                  { name: "React", icon: "react" },
                  { name: "Angular", icon: "angularjs" },
                  { name: "Tailwind", icon: "tailwindcss" },
                  { name: "Bootstrap", icon: "bootstrap" },
                ].map((tech) => (
                  <div key={tech.name} className="flex flex-col items-center group cursor-pointer">
                    <img
                      src={`https://cdn.jsdelivr.net/gh/devicons/devicon/icons/${tech.icon}/${tech.icon}-original.svg`}
                      alt={tech.name}
                      className="w-12 h-12 mb-2 transition-transform group-hover:scale-110 group-hover:-translate-y-1"
                    />
                    <span className="text-sm text-muted-foreground">{tech.name}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Back-End */}
            <div className="bg-card p-8 rounded-lg border border-border">
              <h3 className="text-2xl font-bold mb-6 text-accent">Back-End</h3>
              <div className="grid grid-cols-3 gap-6">
                {[
                  { name: "PHP", icon: "php" },
                  { name: "Laravel", icon: "laravel" },
                  { name: "Symfony", icon: "symfony" },
                  { name: "Java", icon: "java" },
                  { name: "Spring", icon: "spring" },
                ].map((tech) => (
                  <div key={tech.name} className="flex flex-col items-center group cursor-pointer">
                    <img
                      src={`https://cdn.jsdelivr.net/gh/devicons/devicon/icons/${tech.icon}/${tech.icon}-original.svg`}
                      alt={tech.name}
                      className="w-12 h-12 mb-2 transition-transform group-hover:scale-110 group-hover:-translate-y-1"
                    />
                    <span className="text-sm text-muted-foreground">{tech.name}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Database & Tools */}
            <div className="bg-card p-8 rounded-lg border border-border">
              <h3 className="text-2xl font-bold mb-6 text-green-400">Base de données & Outils</h3>
              <div className="grid grid-cols-3 gap-6">
                {[
                  { name: "MySQL", icon: "mysql" },
                  { name: "PostgreSQL", icon: "postgresql" },
                  { name: "MongoDB", icon: "mongodb" },
                  { name: "Git", icon: "git" },
                  { name: "Docker", icon: "docker" },
                  { name: "Figma", icon: "figma" },
                ].map((tech) => (
                  <div key={tech.name} className="flex flex-col items-center group cursor-pointer">
                    <img
                      src={`https://cdn.jsdelivr.net/gh/devicons/devicon/icons/${tech.icon}/${tech.icon}-original.svg`}
                      alt={tech.name}
                      className="w-12 h-12 mb-2 transition-transform group-hover:scale-110 group-hover:-translate-y-1"
                    />
                    <span className="text-sm text-muted-foreground">{tech.name}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Projects Section */}
      <section id="projets" className="py-20 bg-card/30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-4xl font-bold mb-12 text-center gradient-text">Mes Projets</h2>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {projects.map((project) => (
              <ProjectCard key={project.title} project={project} />
            ))}
          </div>
        </div>
      </section>

      {/* Contact Section */}
      <section id="contact" className="py-20">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-4xl font-bold mb-6 gradient-text">Contactez-moi</h2>
          <p className="text-xl text-muted-foreground mb-12">
            Vous avez un projet en tête ? N'hésitez pas à me contacter !
          </p>
          <div className="space-y-6">
            <a
              href="mailto:haissouneabdellatif749@gmail.com"
              className="block bg-card hover:bg-card/80 p-6 rounded-lg border border-border transition"
            >
              <p className="text-sm text-muted-foreground mb-2">Email</p>
              <p className="text-lg text-primary">haissouneabdellatif749@gmail.com</p>
            </a>
            <a
              href="tel:+212690732817"
              className="block bg-card hover:bg-card/80 p-6 rounded-lg border border-border transition"
            >
              <p className="text-sm text-muted-foreground mb-2">Téléphone</p>
              <p className="text-lg text-primary">+212 690732817</p>
            </a>
            <div className="flex justify-center space-x-6 pt-6">
              <a
                href="https://github.com/AbdellatifHissoune"
                target="_blank"
                rel="noreferrer"
                className="bg-card hover:bg-card/80 p-4 rounded-lg border border-border transition"
              >
                <Github className="w-8 h-8" />
              </a>
              <a
                href="https://linkedin.com/in/abdellatif-hissoune"
                target="_blank"
                rel="noreferrer"
                className="bg-card hover:bg-card/80 p-4 rounded-lg border border-border transition"
              >
                <Linkedin className="w-8 h-8" />
              </a>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-background py-8 border-t border-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <p className="text-muted-foreground">© 2025 Abdellatif Hissoune. Tous droits réservés.</p>
        </div>
      </footer>
    </main>
  )
}

// Project Card Component with Modal
function ProjectCard({ project }: { project: (typeof projects)[0] }) {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <>
      <div
        onClick={() => setIsOpen(true)}
        className="group cursor-pointer bg-card p-6 rounded-lg border border-border hover:border-primary/50 transition-all hover:-translate-y-2"
      >
        <h3 className="text-xl font-bold mb-3 text-primary group-hover:underline">{project.title}</h3>
        <p className="text-secondary-foreground mb-4 text-sm line-clamp-2">{project.description}</p>
        <div className="flex flex-wrap gap-3 mb-4">
          {project.logos.map((logo, idx) => (
            <img key={idx} src={logo || "/placeholder.svg"} alt="tech" className="w-8 h-8" />
          ))}
        </div>
        <div className="flex flex-wrap gap-2">
          {project.technologies.map((tech) => (
            <span key={tech} className="bg-background px-3 py-1 rounded-full text-xs border border-border">
              {tech}
            </span>
          ))}
        </div>
        <button className="text-xs text-primary font-medium flex items-center gap-1 mt-4 group-hover:gap-2 transition-all">
          Cahier des charges →
        </button>
      </div>

      {/* Modal - Cahier des Charges */}
      {isOpen && (
        <div
          className="fixed inset-0 z-[60] flex items-center justify-center p-4 modal-overlay"
          onClick={() => setIsOpen(false)}
        >
          <div
            className="bg-card w-full max-w-2xl rounded-lg border border-border shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6 border-b border-border flex justify-between items-center">
              <h2 className="text-2xl font-bold gradient-text">{project.title}</h2>
              <button
                onClick={() => setIsOpen(false)}
                className="text-muted-foreground hover:text-foreground transition"
              >
                <X className="w-6 h-6" />
              </button>
            </div>
            <div className="p-6 overflow-y-auto max-h-[70vh]">
              <div className="space-y-6">
                <section>
                  <h4 className="text-lg font-semibold mb-2 flex items-center gap-2">
                    <Target className="w-5 h-5 text-primary" /> Objectifs du projet
                  </h4>
                  <p className="text-secondary-foreground">{project.description}</p>
                </section>
                <section>
                  <h4 className="text-lg font-semibold mb-2 flex items-center gap-2">
                    <Cpu className="w-5 h-5 text-primary" /> Stack Technique
                  </h4>
                  <div className="flex flex-wrap gap-2">
                    {project.technologies.map((tech) => (
                      <span key={tech} className="bg-background px-3 py-1 rounded-full text-xs border border-border">
                        {tech}
                      </span>
                    ))}
                  </div>
                </section>
                <section>
                  <h4 className="text-lg font-semibold mb-2 flex items-center gap-2">
                    <ListChecks className="w-5 h-5 text-primary" /> Cahier des Charges
                  </h4>
                  <ul className="list-disc list-inside text-secondary-foreground space-y-2 text-sm">
                    {project.cahier.map((item, idx) => (
                      <li key={idx}>{item}</li>
                    ))}
                  </ul>
                </section>
              </div>
            </div>
            <div className="p-6 border-t border-border bg-background/50 text-right">
              <button
                onClick={() => setIsOpen(false)}
                className="bg-primary text-primary-foreground px-6 py-2 rounded-lg font-medium hover:bg-primary/90 transition"
              >
                Fermer
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
