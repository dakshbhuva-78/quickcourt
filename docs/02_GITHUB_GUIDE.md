# 2. Push QuickCourt to GitHub (step by step)

## 2.1 One-time setup
```bash
git config --global user.name  "Your Name"
git config --global user.email "you@example.com"
```
Create a free account at https://github.com.

## 2.2 Create the repository
1. GitHub → **New repository** → name `quickcourt` → **Private** (or Public if your faculty asks) → **do NOT** tick "Add README/.gitignore" (we already have them) → **Create**.
2. Copy the repository URL, e.g. `https://github.com/<your-username>/quickcourt.git`.

## 2.3 Push your code
Run inside the project folder (the one with `pom.xml`):
```bash
git init
git add .
git status                      # CHECK: .env and application-local.properties must NOT be listed
git commit -m "Initial commit: QuickCourt MVP"
git branch -M main
git remote add origin https://github.com/<your-username>/quickcourt.git
git push -u origin main
```
GitHub no longer accepts account passwords on the command line. When asked to log in, use the browser sign-in popup (Git Credential Manager) or a **Personal Access Token** (GitHub → Settings → Developer settings → Tokens).

### Using VS Code instead
Source Control icon (left bar) → **Initialize Repository** → type a message → **Commit** → **Publish Branch** (VS Code creates the GitHub repository for you).

## 2.4 SECURITY: never push secrets
The brief says teams that leak credentials lose marks. This project is already prepared:
- `.gitignore` excludes `.env`, `application-local.properties`, `logs/`, `target/`.
- `application.properties` only contains `${ENV_VAR:default}` placeholders.
- Before every push run `git status` and look at the file list.
- If you ever commit a password by mistake: **change the password immediately** (deleting the file later does not remove it from history).

## 2.5 Teamwork (every member needs commits)
```bash
git pull                         # get latest changes
git checkout -b feature/owner-dashboard
# ... edit files ...
git add .
git commit -m "Add owner dashboard charts"
git push -u origin feature/owner-dashboard
```
Then open a **Pull Request** on GitHub, let a teammate review, and **Merge**. Suggested split: Java lead (service/model), Frontend/e-commerce lead (static pages), QA lead (tests + defect log), Cloud lead (Docker, CI/CD, AWS), Startup lead (business docs, pitch deck).

## 2.6 CI/CD on GitHub
The file `.github/workflows/ci-cd.yml` runs automatically on every push: it compiles the project and runs all tests. Open the **Actions** tab to see green/red results — screenshot this as evidence for the testing and cloud parts.

To enable Docker push + auto-deploy later, add (Settings → Secrets and variables → Actions):
- Secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `SERVER_HOST`, `SERVER_USER`, `SERVER_SSH_KEY`
- Variable: `ENABLE_DEPLOY` = `true`
