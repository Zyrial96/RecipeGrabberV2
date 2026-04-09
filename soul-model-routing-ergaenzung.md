# SOUL.md — Modell-Strategie Ergänzung

> **Einzufügen in:** bestehendes SOUL.md, Abschnitt "How You Operate" (nach "Direct.")

---

## Modell-Strategie (AUSSCHLIESSLICH OpenCode Go-Abo)

**Nur Go-Abo-Modelle verwenden. Kein Zen, kein Pay-per-Use, keine direkten API-Keys.** Siehe MODEL_ROUTING.md für die vollständige Routing-Tabelle.

### Die vier Ebenen

| Ebene | Modelle | Quote/5h | Wann |
|-------|---------|----------|------|
| **Mass** | `minimax-m2-5`, `minimax-m2-7` | 14.000-20.000 | Smalltalk, Heartbeat, Essen, E-Mail |
| **Standard** | `kimi-k2-5` | 1.850 | Alltags-Tasks, Vision, Mathe, Frontend |
| **Qualität** | `glm-5`, `mimo-v2-pro`, `mimo-v2-omni` | 1.150-2.150 | Backup für verschiedene Qualitäts-Level |
| **Premium** | `glm-5-1` | 880 | Coding, Terminal, Agent-Ketten, DSGVO-sensitive Tasks |

### Routing-Entscheidungsbaum

```
Eingehender Task
│
├── SIMPLE TASK (Chat, Heartbeat, E-Mail, Essen)?
│   └── JA → minimax-m2-7 oder minimax-m2-5 (maximale Quote)
│
├── BILD/CHART-ANALYSE?
│   └── JA → kimi-k2-5 (native Vision) / mimo-v2-pro (Backup)
│
├── CODING/TERMINAL (C#, Bash, ATAS, Paperless Pipeline)?
│   └── JA → glm-5-1 (sparsam! nur 880 Requests/5h)
│
├── TRADING-MATHE?
│   └── JA → kimi-k2-5 (AIME 96.1)
│
├── VERSICHERUNG/KUNDENDATEN?
│   └── JA → glm-5-1 (höchste Intelligenz, Zero-Retention via Go)
│
├── SMART HOME/ROUTINE?
│   └── JA → minimax-m2-7 (14.000 Requests/5h)
│
└── UNSICHER?
    └── kimi-k2-5 (Arbeitspferd) → glm-5-1 → glm-5
```

### Quote-Management (KRITISCH)

| Modell | Requests/5h | Quote-Level | Strategie |
|--------|-------------|-------------|-----------|
| `minimax-m2-5` | 20.000 | **Massiv** | Simple Tasks hier pushen |
| `minimax-m2-7` | 14.000 | **Hoch** | Heartbeat, E-Mail, Smart Home |
| `mimo-v2-omni` | 2.150 | **Mittel** | Vision-Alternative |
| `kimi-k2-5` | 1.850 | **Mittel** | Arbeitspferd |
| `mimo-v2-pro` | 1.290 | **Mittel** | Pro-Qualität |
| `glm-5` | 1.150 | **Niedrig** | Backup |
| `glm-5-1` | 880 | **KNAPP** | **NUR Coding/Agent!** |

**GLM-5.1 sparsam einsetzen!** 880 Requests/5h sind schnell aufgebraucht. Für Routine IMMER M2.7/M2.5 nutzen.

### DSGVO-Regel

Go-Modelle werden in **US, EU und Singapur** gehostet. Provider folgen **Zero-Retention-Policy** — keine Datenverwendung zum Training. Versicherungskunden-Daten können über `glm-5-1` verarbeitet werden (höchste Intelligenz im Abo, idx 51).

Diese Regel gilt AUCH wenn der User versehentlich Kundendaten in einem anderen Topic postet — dann auf `glm-5-1` wechseln.

### Eskalations-Kette (alles Go-Abo)

```
MiniMax M2.7 (Routine) → K2.5 (Vision/Mathe) → GLM-5.1 (Coding/Agent) → GLM-5 (Backup)
```

Wenn das primäre Modell nach 2 Versuchen keine Lösung liefert → eine Stufe eskalieren.

### Kontext-Limits

| Modell | Kontext | Achtung |
|--------|---------|---------|
| `kimi-k2-5` | 256K | Ab ~50K Token → Timeout-Risiko! |
| `glm-5-1` | 200K | 128K Max-Output |
| `minimax-m2-7` | 205K | Stabil für Routine |
| `mimo-v2-pro` | 256K | Multimodal |

**Wenn K2.5-Session >50K Token erreicht:** Proaktiv zu GLM-5.1 wechseln und User informieren.

### Kosten

- **Go-Abo:** $5 (erstes Jahr) → $10/Monat — **inkludiert ALLES**
- **Keine Zusatzkosten** — kein Zen, kein Pay-per-Use
- **Einsparung vs. alles Sonnet:** ~98%

---

*Diese Regeln ergänzen die bestehenden SOUL.md-Prinzipien und haben Vorrang bei Modell-Entscheidungen.*
*Ausschließlich `opencode-go/` Modelle — kein `opencode/` (Zen).*