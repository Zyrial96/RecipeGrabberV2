# MODEL_ROUTING.md — Modell-Strategie für OpenClaw

> **Ziel:** Jeden Task beim besten verfügbaren Modell ausführen — AUSSCHLIESSLICH OpenCode Go-Abo.
> **Kein Zen. Kein Pay-per-Use. Nur Go.**
> **Grundlage:** Benchmarks Stand April 2026
> **Privacy:** Go-Modelle werden in US/EU/SG gehostet, Zero-Retention-Policy, keine Trainingsdatenverwendung. DSGVO-kompatibel.

---

## Verfügbare Modelle (OpenCode Go-Abo — $5→$10/Monat, inkludiert)

| Modell | Model ID | Requests/5h | Stärken | idx |
|--------|----------|-------------|---------|-----|
| **GLM-5.1** | `opencode-go/glm-5-1` | 880 | Terminal-Bench 63.5, CyberGym 68.7, Tool-Decathlon 40.7, 8h Agent Tasks | 51 |
| **GLM-5** | `opencode-go/glm-5` | 1.150 | Starkes Basismodell, Index 50 | 50 |
| **Kimi K2.5** | `opencode-go/kimi-k2-5` | 1.850 | Native Vision, AIME 96.1, Frontend, Agent Swarm | 47 |
| **MiMo-V2-Pro** | `opencode-go/mimo-v2-pro` | 1.290 | Multimodal, Index 49 | 49 |
| **MiMo-V2-Omni** | `opencode-go/mimo-v2-omni` | 2.150 | Multimodal, Index 43-45, hohe Quote | 43 |
| **MiniMax M2.7** | `opencode-go/minimax-m2-7` | 14.000 | Index 50, extrem hohe Quote | 50 |
| **MiniMax M2.5** | `opencode-go/minimax-m2-5` | 20.000 | Höchste Quote, Index 50 | 50 |

### Usage-Limits (Go-Abo)
- **5 Stunden:** $12 Wert
- **Wöchentlich:** $30 Wert
- **Monatlich:** $60 Wert
- Limits in Dollar → tatsächliche Request-Anzahl hängt vom Modell

### DSGVO
Go-Modelle werden in **US, EU und Singapur** gehostet mit **Zero-Retention-Policy**. Keine Daten werden zum Training verwendet. DSGVO-kompatibel — keine zusätzlichen Zen-Kosten nötig.

---

## Modell-Profile

### kimi-k2-5 (`opencode-go/kimi-k2-5`)
- **Stärken:** Native Vision, AIME 96.1, Frontend-Ästhetik, Agent Swarm
- **Schwächen:** Index 47, Session-Timeouts bei >50K Token
- **Quote:** 1.850/5h — bestes Preis/Leistungs-Verhältnis im Abo
- **Einsatz:** Arbeitspferd für 80% der Tasks

### glm-5-1 (`opencode-go/glm-5-1`)
- **Stärken:** Terminal-Bench 63.5, CyberGym 68.7, BrowseComp 68.0, 8h Agent Tasks, Index 51
- **Schwächen:** Kein native Vision, **880 Requests/5h (knappste Quote!)**
- **Einsatz:** Coding, Agent-Ketten, Terminal — sparsam einsetzen!

### glm-5 (`opencode-go/glm-5`)
- **Stärken:** Index 50, 1.150/5h — guter Mittelweg
- **Einsatz:** GLM-5.1-Backup wenn Quote aufgebraucht

### mimo-v2-pro (`opencode-go/mimo-v2-pro`)
- **Stärken:** Index 49, Multimodal, 1.290/5h
- **Einsatz:** Vision-Alternative zu K2.5

### minimax-m2-7 (`opencode-go/minimax-m2-7`)
- **Stärken:** Index 50, 14.000/5h — extrem hohe Quote
- **Einsatz:** Heartbeat, E-Mail, Smart Home, einfach alles wo Quote > Qualität

### minimax-m2-5 (`opencode-go/minimax-m2-5`)
- **Stärken:** 20.000/5h — maximale Quote
- **Einsatz:** Simpelste Tasks, Essen, Smalltalk

---

## Routing-Regeln

### Regel 1: Quote bewusst einsetzen
GLM-5.1 hat nur 880 Requests/5h — das ist schnell aufgebraucht. Für Routine-Tasks IMMER M2.7 oder M2.5 nutzen. GLM-5.1 nur für Coding/Agent-Tasks.

### Regel 2: Task-zu-Modell-Mapping (NUR Go-Abo)

| Task-Typ | Primär | Fallback | Begründung |
|----------|--------|----------|-------------|
| **Routine-Chats, Smalltalk, Erinnerungen** | `opencode-go/minimax-m2-7` | `opencode-go/kimi-k2-5` | 14.000/5h — Quote-Pusher |
| **Trading-Mathe & Chart-Analyse** | `opencode-go/kimi-k2-5` | `opencode-go/mimo-v2-pro` | AIME 96.1 + native Vision |
| **Smart Home Scripts / Bash / Docker** | `opencode-go/minimax-m2-7` | `opencode-go/kimi-k2-5` | Routine, Quote wichtig |
| **E-Mail-Verarbeitung / Templates** | `opencode-go/minimax-m2-7` | `opencode-go/kimi-k2-5` | Text reicht, Quote schonen |
| **Kochen / Essen / Rezepte** | `opencode-go/minimax-m2-5` | `opencode-go/minimax-m2-7` | Simpelste Tasks, 20.000/5h |
| **Reiseplanung / Buchung** | `opencode-go/kimi-k2-5` | `opencode-go/mimo-v2-pro` | Vision + Web |
| **Paperless OCR / Dokumente** | `opencode-go/kimi-k2-5` | `opencode-go/glm-5-1` | Vision/OCR, GLM für Pipelines |
| **Android-Entwicklung (Kotlin)** | `opencode-go/glm-5-1` | `opencode-go/glm-5` | CyberGym 68.7, beste Coding-Qualität |
| **Web-Dev Backend (Next.js)** | `opencode-go/glm-5-1` | `opencode-go/kimi-k2-5` | Terminal-Bench++, Agent-Tasks |
| **Web-Dev Frontend (React)** | `opencode-go/kimi-k2-5` | `opencode-go/glm-5-1` | K2.5 hat beste Frontend-Ästhetik |
| **C# / ATAS / Trading-Indikatoren** | `opencode-go/glm-5-1` | `opencode-go/glm-5` | Terminal-Bench 63.5, CyberGym 68.7 |
| **Versicherung / Kunden-Daten** | `opencode-go/glm-5-1` | `opencode-go/kimi-k2-5` | Höchste Intelligenz (idx 51), Zero-Retention via Go |
| **Lange Agent-Task-Ketten** | `opencode-go/glm-5-1` | `opencode-go/glm-5` | 8h Agent Tasks gebaut |
| **Komplexes Reasoning** | `opencode-go/glm-5-1` | `opencode-go/kimi-k2-5` | Index 51, höchste Intelligenz im Abo |
| **Screenshots / Bildanalyse** | `opencode-go/kimi-k2-5` | `opencode-go/mimo-v2-pro` | K2.5 native Vision, MiMo Alternative |
| **Code-Review / Bug-Fixing** | `opencode-go/glm-5-1` | `opencode-go/glm-5` | CyberGym dominiert |
| **Heartbeat / Cron / Status** | `opencode-go/minimax-m2-7` | `opencode-go/minimax-m2-5` | 14.000-20.000/5h für Routine |

### Regel 3: DSGVO (Go-Abo ist kompatibel)
Go-Modelle werden in US/EU/SG gehostet mit Zero-Retention-Policy. **Versicherungskunden-Daten können über Go-Modelle verarbeitet werden.** Kein separater Zen-Export nötig. GLM-5.1 (Index 51) für DSGVO-sensitive Tasks verwenden.

### Regel 4: Quote-Management

| Modell | Requests/5h | Quote-Level | Strategie |
|--------|-------------|-------------|-----------|
| `minimax-m2-5` | 20.000 | **Massiv** | Push simple Tasks hier |
| `minimax-m2-7` | 14.000 | **Hoch** | Heartbeat, E-Mail, Smart Home |
| `mimo-v2-omni` | 2.150 | **Mittel** | Vision-Alternative |
| `kimi-k2-5` | 1.850 | **Mittel** | Arbeitspferd |
| `mimo-v2-pro` | 1.290 | **Mittel** | Pro-Qualität |
| `glm-5` | 1.150 | **Niedrig** | Backup |
| `glm-5-1` | 880 | **Knapp** | NUR Coding/Agent |

**GLM-5.1 sparsam einsetzen!** 880 Requests/5h sind schnell aufgebraucht.

### Regel 5: Eskalations-Kette (alles im Go-Abo)
```
MiniMax M2.7 (Routine) → K2.5 (Vision/Mathe) → GLM-5.1 (Coding/Agent) → GLM-5 (Backup)
```
Wenn das primäre Modell nach 2 Versuchen keine Lösung liefert → eine Stufe eskalieren.

### Regel 6: Kontext-Limits

| Modell | Kontext | Achtung |
|--------|---------|---------|
| `opencode-go/kimi-k2-5` | 256K | Ab ~50K Token → Timeout-Risiko |
| `opencode-go/glm-5-1` | 200K | 128K Max-Output, stabil für Agent |
| `opencode-go/glm-5` | 200K | 128K Max-Output |
| `opencode-go/minimax-m2-7` | 205K | Stabil für Routine |
| `opencode-go/mimo-v2-pro` | 256K | Multimodal |

**Wenn K2.5-Session >50K Token erreicht:** Proaktiv zu GLM-5.1 wechseln und User informieren.

---

## Agent-spezifische Konfiguration (NUR Go-Abo)

### main (General-Agent)
- **Primär:** `opencode-go/kimi-k2-5`
- **Fallback:** `opencode-go/minimax-m2-7` → `opencode-go/glm-5-1`
- **Begründung:** K2.5 Arbeitspferd, M2.7 Routine-Backup, GLM-5.1 für komplexere Tasks.

### coding (Topic 19)
- **Primär:** `opencode-go/glm-5-1`
- **Fallback:** `opencode-go/glm-5` → `opencode-go/kimi-k2-5`
- **Begründung:** CyberGym 68.7, Terminal-Bench 63.5. Achtung: nur 880 Requests/5h!

### trading (Topic 2)
- **Primär:** `opencode-go/kimi-k2-5`
- **Fallback:** `opencode-go/mimo-v2-pro`
- **Begründung:** AIME 96.1, native Vision für Charts.

### versicherung (Topic 3)
- **Primär:** `opencode-go/glm-5-1`
- **Fallback:** `opencode-go/kimi-k2-5`
- **Begründung:** Höchste Intelligenz im Abo (idx 51), Zero-Retention via Go, DSGVO-kompatibel.

### smarthome (Topic 5)
- **Primär:** `opencode-go/minimax-m2-7`
- **Fallback:** `opencode-go/kimi-k2-5`
- **Begründung:** Routine Bash/HA, 14.000 Requests/5h für Heartbeat+Cron.

### reise (Topic 20)
- **Primär:** `opencode-go/kimi-k2-5`
- **Fallback:** `opencode-go/mimo-v2-pro`
- **Begründung:** Vision + Web-Suche.

### email (Topic 21)
- **Primär:** `opencode-go/minimax-m2-7`
- **Fallback:** `opencode-go/kimi-k2-5`
- **Begründung:** Textverarbeitung, Quote schonen.

### essen (Topic 80)
- **Primär:** `opencode-go/minimax-m2-5`
- **Fallback:** `opencode-go/minimax-m2-7`
- **Begründung:** Simpelste Tasks, 20.000 Requests/5h.

### paperless (Topic 637)
- **Primär:** `opencode-go/kimi-k2-5`
- **Fallback:** `opencode-go/glm-5-1`
- **Begründung:** K2.5 für Vision/OCR, GLM-5.1 für Pipeline-Agent-Ketten.

### shopping (Topic 945)
- **Primär:** `opencode-go/kimi-k2-5`
- **Fallback:** `opencode-go/mimo-v2-pro`
- **Begründung:** Web-Suche + Bildanalyse.

---

## Kosten-Projektion

### Go-Abo: $5 (erstes Jahr) → $10/Monat
- **Alle Modelle inkludiert** mit Usage-Limits ($12/5h, $30/Woche, $60/Monat)
- **Keine zusätzlichen Zen-Kosten** — 100% Go-Abo
- **Vormals (alles Sonnet @ Zen-Preisen):** ~$200-300/Monat
- **Einsparung: ~98%**

---

## SOUL.md-Ergänzung

Füge in SOUL.md unter "How You Operate" hinzu:

```markdown
### Modell-Strategie (NUR OpenCode Go-Abo)

**Ausschließlich Go-Abo-Modelle verwenden. Kein Zen, kein Pay-per-Use.** Siehe MODEL_ROUTING.md.

- **Routine** → `minimax-m2-7` oder `minimax-m2-5` (maximale Quote)
- **Vision/Mathe/Alltag** → `kimi-k2-5` (Arbeitspferd, native Vision)
- **Coding/Agent** → `glm-5-1` (höchste Intelligenz, sparsam! nur 880 Requests/5h)
- **Vision-Alternative** → `mimo-v2-pro` (wenn K2.5-Quote niedrig)

**Eskalation:** M2.7 → K2.5 → GLM-5.1 → GLM-5 (alles im Go-Abo)

**Quote-Regel:** GLM-5.1 sparsam einsetzen (880/5h). Routine immer M2.7/M2.5.

**DSGVO:** Go-Modelle sind Zero-Retention in US/EU/SG. Kundendaten sind sicher.
```

---

## Aktualisierungs-Protokoll

1. **Monatlich:** Benchmark-Daten prüfen und Routing anpassen
2. **Bei neuen Go-Abo-Modellen:** Sofort MODEL_ROUTING.md updaten
3. **Kosten-Tracking:** Go-Usage im OpenCode-Dashboard monatlich prüfen

---

*Letzte Aktualisierung: April 2026*
*Alle Modell-IDs verwenden das `opencode-go/` Präfix — ausschließlich Go-Abo.*