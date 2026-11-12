# University Courses Platform

Proiect pentru disciplina **Sisteme de Control Distribuit (SCD)** – platformă pentru gestionarea
orarului unei facultăți, cu generarea automată a orarului, notificări în timp real și asistent AI.

## Componente

| Folder              | Ce conține                                   | Tehnologii                         |
|---------------------|----------------------------------------------|------------------------------------|
| `course-platform/`  | Serverul (API REST + WebSocket)              | Java 21, Spring Boot 4, MySQL      |
| `admin-frontend/`   | Panoul web al administratorului              | Vue 3, Vuetify 4, TypeScript       |
| _(urmează)_         | Aplicația mobilă pentru studenți și profesori | Android                           |

## Pornire rapidă

1. **MySQL** – serviciul MySQL 8.4 trebuie să ruleze pe portul 3306.
2. **Backend** – în `course-platform/src/main/resources` creează `application-local.properties` cu
   `spring.datasource.password=PAROLA_TA`, apoi pornește `CoursePlatformApplication` din IntelliJ.
   La prima pornire baza `course_platform` se creează și se populează automat.
3. **Frontend** – în `admin-frontend/`: `npm install`, apoi `npm run dev` și deschide
   http://localhost:5173.

Conturi demo (parola `parola123`): `admin@utcn.ro`, `profesor.curs.scd.a@utcn.ro`,
`asistent.lab.scd@utcn.ro`, `student1@student.utcn.ro`.

## Testarea API-ului

Colecția Postman se află în `course-platform/postman/`.
