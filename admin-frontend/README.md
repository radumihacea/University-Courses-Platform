# UCP Admin – panoul web al administratorului

Aplicația web (Vue 3 + Vuetify 4 + TypeScript) prin care secretariatul / adminul gestionează
platforma **University Courses Platform**: date de bază, generarea și publicarea orarului,
cererile de modificare ale profesorilor și statisticile.

## Cerințe

- Node.js 22.18+ sau 24.12+
- Backend-ul Spring Boot (`../course-platform`) pornit pe `http://localhost:8080`

## Pornire

```bash
npm install
npm run dev
```

Aplicația pornește pe **http://localhost:5173** (portul este fix, pentru că backend-ul permite
CORS doar pentru această adresă).

## Scripturi

| Comandă              | Ce face                                              |
|----------------------|------------------------------------------------------|
| `npm run dev`        | Pornește aplicația în modul de dezvoltare             |
| `npm run type-check` | Verifică tipurile TypeScript (`vue-tsc`)              |
| `npm run build`      | Verifică tipurile și construiește versiunea finală în `dist/` |
| `npm run preview`    | Servește local versiunea din `dist/`                  |

## Configurare (opțional)

Valorile implicite merg fără nicio configurare. Pentru alte valori, copiază `.env.example`
ca `.env.local` (ignorat de Git) și modifică:

- `VITE_API_URL` – adresa backend-ului

## Structura

```
src/
├── api/                 Comunicarea cu backend-ul
│   ├── http.ts          Clientul axios + mesaje de eroare clare
│   ├── crud.ts          Operațiile CRUD generice pentru toate entitățile
│   ├── schedule.ts      Orar: generare, publicare, conflicte, săli libere, statistici
│   └── types.ts         Tipurile datelor (corespund DTO-urilor din backend)
├── components/
│   ├── common/          Componente reutilizabile (antet pagină, carduri, confirmări, mesaje)
│   ├── crud/            Pagina CRUD generică (tabel + formular + ștergere)
│   ├── notifications/   Clopoțelul de notificări live
│   └── timetable/       Grila săptămânală a orarului
├── composables/         Logică reutilizabilă: temă, notificări WebSocket, liste, mesaje
├── layouts/             Layout-ul cu meniul lateral și bara de sus
├── plugins/vuetify.ts   Tema (culorile UTCN, mod luminos/întunecat) și setările implicite
├── router/              Paginile aplicației
├── styles/              Stiluri globale
├── utils/labels.ts      Etichetele în română pentru valorile din backend
└── views/               Paginile (Orar, Cereri, Statistici + 12 pagini CRUD)
```

## Autentificare

Panoul este accesibil doar conturilor cu rolul **ADMIN** (cont demo: `admin@utcn.ro` / `parola123`).
La login, backend-ul întoarce un token semnat, salvat în browser și trimis la fiecare cerere
(`Authorization: Bearer ...`). Când tokenul expiră (după 12 ore), aplicația cere din nou autentificarea.

## Funcționalități

- **Orar** – generare automată, verificarea conflictelor, publicare, vizualizare pe
  semigrupă / profesor / sală, detalii pentru fiecare oră și sălile libere din același interval
- **Cereri de modificare** – aprobare (cu verificare automată de conflicte) sau respingere
- **Statistici** – încărcarea profesorilor și gradul de ocupare al sălilor
- **CRUD** pentru clădiri, săli, discipline, serii, grupe, semigrupe, profesori, studenți,
  asignări didactice, restricții și notificări
- **Notificări live** prin WebSocket (ex: o cerere nouă de la un profesor apare instant)
- **Temă luminoasă / întunecată**, memorată în browser
